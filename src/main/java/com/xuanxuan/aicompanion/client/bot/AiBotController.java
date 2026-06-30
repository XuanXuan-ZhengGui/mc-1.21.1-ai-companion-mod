package com.xuanxuan.aicompanion.client.bot;

import com.xuanxuan.aicompanion.client.AiCompanionClient;
import com.xuanxuan.aicompanion.client.ai.AiRouter;
import com.xuanxuan.aicompanion.client.config.AiCompanionConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public final class AiBotController {
    private static final int DECISION_INTERVAL = 60;
    private static final Random RANDOM = new Random();
    private static boolean initialized = false;
    private static int tickCounter = 0;
    private static int decisionCooldown = 0;
    private static AiBotInput currentInput = new AiBotInput();
    private static boolean isThinking = false;
    private static boolean inputReflectionWorks = true;
    private static boolean isFleeing = false;
    private static int fleeTicks = 0;

    private static final AiBotActionQueue actionQueue = new AiBotActionQueue();
    private static String currentGoal = "无";
    private static String lastThought = "";
    private static final List<String> actionHistory = new ArrayList<>();
    private static int historyLimit = 10;

    private AiBotController() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (AiCompanionConfig.aiBotMode()) {
                AiBotLogger.info("已加入世界，AI 控制启动中...");
                AiBotLogger.info("日志系统已启用，仅本客户端可见");
            }
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!AiCompanionConfig.aiBotMode() || overlay) return;
            String text = message.getString();
            if (text.contains("[" + AiPerception.BOT_NAME + "]")) return;
            if (text.contains(AiCompanionConfig.modelName()) || text.contains("玄玄") || text.contains("AI")) {
                onChatReceived(text);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(AiBotController::tick);
    }

    private static void tick(MinecraftClient client) {
        if (!AiCompanionConfig.aiBotMode()) return;
        if (client.player == null || client.world == null) return;

        tickCounter++;

        updateSurvival(client);

        if (isFleeing) {
            applyInput(client);
            return;
        }

        // Execute queued actions
        if (!actionQueue.isEmpty()) {
            executeQueuedAction(client);
            applyInput(client);
            return;
        }

        decisionCooldown--;
        if (decisionCooldown <= 0 && !isThinking) {
            requestDecision(client);
            decisionCooldown = DECISION_INTERVAL + RANDOM.nextInt(20);
        }

        applyInput(client);
    }

    private static void executeQueuedAction(MinecraftClient client) {
        AiBotAction action = actionQueue.tick();
        if (action == null) return;

        ClientPlayerEntity player = client.player;
        if (player == null) return;

        currentInput.reset();

        switch (action.type) {
            case "move" -> {
                switch (action.value) {
                    case "forward" -> currentInput.forward = 1.0f;
                    case "back" -> currentInput.forward = -1.0f;
                    case "left" -> currentInput.sideways = 1.0f;
                    case "right" -> currentInput.sideways = -1.0f;
                    case "stop" -> currentInput.forward = 0.0f;
                }
            }
            case "jump" -> currentInput.jumping = true;
            case "sneak" -> currentInput.sneaking = "on".equals(action.value);
            case "look" -> {
                player.setYaw(action.targetYaw);
                player.setPitch(action.targetPitch);
            }
            case "attack" -> {
                Entity target = findNearestHostile(client);
                if (target != null) {
                    faceEntity(player, target);
                    if (client.interactionManager != null) {
                        client.interactionManager.attackEntity(player, target);
                    }
                }
            }
            case "use" -> {
                if (client.interactionManager != null) {
                    client.interactionManager.interactItem(player, Hand.MAIN_HAND);
                }
            }
            case "chat" -> {
                if (!action.value.isBlank()) {
                    ClientPlayNetworkHandler handler = player.networkHandler;
                    if (handler != null) {
                        handler.sendChatMessage(action.value);
                    }
                }
            }
            case "selectSlot" -> {
                try {
                    int slot = Integer.parseInt(action.value);
                    setSelectedSlot(player, slot);
                } catch (Exception ignored) {
                }
            }
            case "mine" -> {
                if (client.interactionManager != null) {
                    client.interactionManager.attackBlock(player.getBlockPos().add(
                            (int) -Math.sin(Math.toRadians(player.getYaw())),
                            0,
                            (int) Math.cos(Math.toRadians(player.getYaw()))
                    ), net.minecraft.util.math.Direction.UP);
                }
            }
            case "place" -> {
                if (client.interactionManager != null) {
                    client.interactionManager.interactItem(player, Hand.MAIN_HAND);
                }
            }
            case "stop" -> currentInput.reset();
        }
    }

    private static void updateSurvival(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        float health = player.getHealth();
        boolean inDanger = isInDanger(player);

        if (health < 6.0f || inDanger) {
            if (!isFleeing) {
                isFleeing = true;
                fleeTicks = 60;
                actionQueue.clear();
                currentInput.reset();
                currentInput.forward = -1.0f;
                currentInput.sideways = RANDOM.nextFloat() * 2.0f - 1.0f;
                currentInput.jumping = true;
                AiBotLogger.warn("血量低/处于危险！正在逃跑！血量=" + String.format("%.1f", health));
            }
        }

        if (isFleeing) {
            fleeTicks--;
            if (fleeTicks <= 0 || (health >= 12.0f && !inDanger)) {
                isFleeing = false;
                AiBotLogger.info("逃跑结束，状态安全。");
            }
            return;
        }

        tryEatFood(player);
    }

    private static boolean isInDanger(ClientPlayerEntity player) {
        try {
            if (player.isOnFire()) return true;
            BlockPos pos = player.getBlockPos().down();
            String blockId = player.getWorld().getBlockState(pos).getBlock().toString().toLowerCase();
            return blockId.contains("lava") || blockId.contains("fire") || blockId.contains("magma");
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void tryEatFood(ClientPlayerEntity player) {
        try {
            if (player.getHungerManager().getFoodLevel() > 6) return;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (isFood(stack)) {
                    setSelectedSlot(player, i);
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.interactionManager != null) {
                        client.interactionManager.interactItem(player, Hand.MAIN_HAND);
                    }
                    AiBotLogger.action("饥饿值低，自动吃食物 (栏位 " + i + ")");
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean isFood(ItemStack stack) {
        try {
            java.lang.reflect.Method m = stack.getClass().getMethod("getUseAction");
            Object action = m.invoke(stack);
            String name = action.toString();
            return name.equals("EAT") || name.equals("DRINK");
        } catch (Exception e) {
            return false;
        }
    }

    private static void setSelectedSlot(ClientPlayerEntity player, int slot) {
        try {
            Field f = player.getInventory().getClass().getDeclaredField("selectedSlot");
            f.setAccessible(true);
            f.setInt(player.getInventory(), slot);
        } catch (Exception e) {
            try {
                Field f2 = player.getInventory().getClass().getDeclaredField("mainHandSlot");
                f2.setAccessible(true);
                f2.setInt(player.getInventory(), slot);
            } catch (Exception ignored) {
            }
        }
    }

    private static void requestDecision(MinecraftClient client) {
        isThinking = true;
        String perception = AiPerception.describe(client);
        String history = String.join("; ", actionHistory);
        if (history.isBlank()) history = "无";

        String prompt = buildDecisionPrompt(perception, history);
        AiBotLogger.think("正在向模型请求决策...");

        CompletableFuture.supplyAsync(() -> AiRouter.reply(prompt))
                .thenAccept(response -> client.execute(() -> {
                    try {
                        parseJsonDecision(response);
                        AiBotLogger.think("模型决策完成，目标: " + currentGoal);
                        AiBotLogger.info("动作队列: " + actionQueue.size() + " 个动作待执行");
                    } catch (Exception e) {
                        AiBotLogger.warn("决策解析失败: " + e.getMessage());
                        fallbackParse(response);
                    }
                    isThinking = false;
                }));
    }

    private static String buildDecisionPrompt(String perception, String history) {
        return "你是 Minecraft 中的 AI 玩家 " + AiPerception.BOT_NAME + "。\n"
                + "你必须以 JSON 格式回复，格式如下（不要包含任何其他文字，只输出 JSON）:\n"
                + "{\n"
                + "  \"thought\": \"你的思考过程，分析当前环境和应该做什么\",\n"
                + "  \"goal\": \"当前主要目标（如:探索、收集木头、战斗、逃跑等）\",\n"
                + "  \"actions\": [\n"
                + "    {\"type\": \"move\", \"value\": \"forward\", \"duration\": 40},\n"
                + "    {\"type\": \"jump\", \"duration\": 5},\n"
                + "    {\"type\": \"look\", \"yaw\": 90, \"pitch\": 0, \"duration\": 10},\n"
                + "    {\"type\": \"attack\", \"duration\": 20},\n"
                + "    {\"type\": \"use\", \"duration\": 10},\n"
                + "    {\"type\": \"mine\", \"duration\": 60},\n"
                + "    {\"type\": \"place\", \"duration\": 10},\n"
                + "    {\"type\": \"selectSlot\", \"value\": \"0\", \"duration\": 1},\n"
                + "    {\"type\": \"chat\", \"value\": \"要说的话\", \"duration\": 1},\n"
                + "    {\"type\": \"stop\", \"duration\": 20}\n"
                + "  ],\n"
                + "  \"chat\": \"可选的聊天内容\"\n"
                + "}\n"
                + "动作类型说明: move(前进/后退/左/右/停止), jump(跳跃), look(看向指定角度), attack(攻击), use(使用物品), mine(挖掘), place(放置), selectSlot(选择快捷栏), chat(说话), stop(停止)\n"
                + "duration 单位是游戏刻(tick)，20 tick = 1秒。\n"
                + "最近执行过的动作: " + history + "\n"
                + "当前环境:\n" + perception;
    }

    private static void parseJsonDecision(String response) {
        actionQueue.clear();
        actionHistory.clear();

        if (response == null || response.isBlank()) {
            AiBotLogger.warn("模型返回空响应");
            return;
        }

        String json = extractJson(response);
        if (json.isBlank()) {
            AiBotLogger.warn("无法从响应中提取 JSON: " + response.substring(0, Math.min(100, response.length())));
            fallbackParse(response);
            return;
        }

        lastThought = extractJsonString(json, "thought");
        currentGoal = extractJsonString(json, "goal");
        String chatMsg = extractJsonString(json, "chat");

        AiBotLogger.perceive("模型思考: " + lastThought);
        AiBotLogger.perceive("当前目标: " + currentGoal);

        List<AiBotAction> actions = parseActions(json);
        if (actions.isEmpty()) {
            AiBotLogger.warn("模型未返回有效动作");
            return;
        }

        for (AiBotAction a : actions) {
            actionQueue.add(a);
            actionHistory.add(a.type + ":" + a.value);
        }
        while (actionHistory.size() > historyLimit) {
            actionHistory.remove(0);
        }

        if (!chatMsg.isBlank()) {
            actionQueue.add(AiBotAction.chat(chatMsg));
        }

        AiBotLogger.action("已加载 " + actions.size() + " 个动作到队列");
    }

    private static String extractJson(String response) {
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return "";
    }

    private static List<AiBotAction> parseActions(String json) {
        List<AiBotAction> result = new ArrayList<>();
        int arrStart = json.indexOf("\"actions\"");
        if (arrStart < 0) return result;

        int bracketStart = json.indexOf("[", arrStart);
        int bracketEnd = json.indexOf("]", bracketStart);
        if (bracketStart < 0 || bracketEnd < 0) return result;

        String arrayContent = json.substring(bracketStart + 1, bracketEnd);
        List<String> objects = splitJsonObjects(arrayContent);

        for (String obj : objects) {
            AiBotAction action = parseActionObject(obj);
            if (action != null) {
                result.add(action);
            }
        }

        return result;
    }

    private static List<String> splitJsonObjects(String content) {
        List<String> objects = new ArrayList<>();
        int braceCount = 0;
        int start = -1;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') {
                if (braceCount == 0) start = i;
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0 && start >= 0) {
                    objects.add(content.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return objects;
    }

    private static AiBotAction parseActionObject(String obj) {
        try {
            String type = extractJsonString(obj, "type");
            String value = extractJsonString(obj, "value");
            int duration = 20;
            try {
                String durStr = extractNumber(obj, "duration");
                if (!durStr.isBlank()) duration = Integer.parseInt(durStr);
            } catch (Exception ignored) {
            }

            AiBotAction action = new AiBotAction(type, value, duration);

            if ("look".equals(type)) {
                String yawStr = extractNumber(obj, "yaw");
                String pitchStr = extractNumber(obj, "pitch");
                if (!yawStr.isBlank()) action.targetYaw = Float.parseFloat(yawStr);
                if (!pitchStr.isBlank()) action.targetPitch = Float.parseFloat(pitchStr);
            }

            return action;
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractNumber(String json, String key) {
        String marker = "\"" + key + "\"";
        int idx = json.indexOf(marker);
        if (idx < 0) return "";

        int colon = json.indexOf(":", idx);
        if (colon < 0) return "";

        int end = colon + 1;
        while (end < json.length() && (json.charAt(end) == ' ' || json.charAt(end) == '\t')) end++;

        int valStart = end;
        while (end < json.length()) {
            char c = json.charAt(end);
            if ((c >= '0' && c <= '9') || c == '-' || c == '.' || c == 'e' || c == 'E') {
                end++;
            } else {
                break;
            }
        }

        if (valStart < end) {
            return json.substring(valStart, end);
        }
        return "";
    }

    private static String extractJsonString(String json, String key) {
        String marker = "\"" + key + "\"";
        int keyIndex = json.indexOf(marker);
        if (keyIndex < 0) {
            return "";
        }

        int start = json.indexOf('"', keyIndex + marker.length());
        if (start < 0) {
            return "";
        }

        StringBuilder value = new StringBuilder();
        boolean escaping = false;
        for (int i = start + 1; i < json.length(); i++) {
            char character = json.charAt(i);
            if (escaping) {
                value.append(switch (character) {
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    default -> character;
                });
                escaping = false;
            } else if (character == '\\') {
                escaping = true;
            } else if (character == '"') {
                return value.toString();
            } else {
                value.append(character);
            }
        }
        return "";
    }

    private static void fallbackParse(String response) {
        currentInput.reset();
        if (response == null || response.isBlank()) return;

        String lower = response.toLowerCase();
        actionQueue.clear();

        if (lower.contains("前进") || lower.contains("forward")) {
            actionQueue.add(AiBotAction.move("forward", 40));
        } else if (lower.contains("后退") || lower.contains("back")) {
            actionQueue.add(AiBotAction.move("back", 40));
        } else if (lower.contains("左") || lower.contains("left")) {
            actionQueue.add(AiBotAction.move("left", 40));
        } else if (lower.contains("右") || lower.contains("right")) {
            actionQueue.add(AiBotAction.move("right", 40));
        }

        if (lower.contains("跳") || lower.contains("jump")) {
            actionQueue.add(AiBotAction.jump(10));
        }

        if (lower.contains("攻击") || lower.contains("attack")) {
            actionQueue.add(AiBotAction.attack(20));
        }

        int chatIdx = response.indexOf("聊天:");
        if (chatIdx < 0) chatIdx = response.indexOf("chat:");
        if (chatIdx >= 0) {
            String msg = response.substring(chatIdx + 5).trim().split("\\n")[0];
            if (msg.length() > 100) msg = msg.substring(0, 100);
            actionQueue.add(AiBotAction.chat(msg));
        }

        AiBotLogger.info("使用备用解析，加载 " + actionQueue.size() + " 个动作");
    }

    private static void applyInput(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        if (inputReflectionWorks) {
            inputReflectionWorks = tryApplyInputReflection(player);
        }
        if (!inputReflectionWorks) {
            applyInputDirect(player);
        }
    }

    private static boolean tryApplyInputReflection(ClientPlayerEntity player) {
        try {
            Field inputField = ClientPlayerEntity.class.getDeclaredField("input");
            inputField.setAccessible(true);
            Object input = inputField.get(player);
            if (input == null) return false;

            boolean forward = trySetFloat(input, "movementForward", currentInput.forward)
                    || trySetFloat(input, "forwardMovement", currentInput.forward);
            boolean sideways = trySetFloat(input, "movementSideways", currentInput.sideways)
                    || trySetFloat(input, "sidewaysMovement", currentInput.sideways);
            trySetBoolean(input, "jumping", currentInput.jumping);
            trySetBoolean(input, "sneaking", currentInput.sneaking);

            return forward || sideways;
        } catch (Exception e) {
            return false;
        }
    }

    private static void applyInputDirect(ClientPlayerEntity player) {
        double yawRad = Math.toRadians(player.getYaw());
        double forward = currentInput.forward;
        double sideways = currentInput.sideways;

        double speed = currentInput.sneaking ? 0.03 : (player.isSprinting() ? 0.13 : 0.1);
        double vx = -(sideways * Math.sin(yawRad) + forward * Math.cos(yawRad)) * speed;
        double vz = (sideways * Math.cos(yawRad) - forward * Math.sin(yawRad)) * speed;
        double vy = player.getVelocity().y;

        if (currentInput.jumping && player.isOnGround()) {
            vy = 0.42;
        }

        player.setVelocity(vx, vy, vz);
    }

    private static Entity findNearestHostile(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return null;
        double bestDist = 25.0;
        Entity best = null;
        for (Entity e : client.world.getEntities()) {
            if (e instanceof HostileEntity && e != player) {
                double dist = player.squaredDistanceTo(e);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = e;
                }
            }
        }
        return best;
    }

    private static void faceEntity(ClientPlayerEntity player, Entity target) {
        Vec3d eye = player.getEyePos();
        Vec3d targetEye = target.getEyePos();
        double dx = targetEye.x - eye.x;
        double dy = targetEye.y - eye.y;
        double dz = targetEye.z - eye.z;
        double distXZ = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(-dy, distXZ));
        player.setYaw(yaw);
        player.setPitch(pitch);
    }

    private static void onChatReceived(String message) {
        if (isThinking) return;
        isThinking = true;
        long delay = 500 + RANDOM.nextInt(1500);
        AiBotLogger.think("检测到聊天提及，准备回复...");

        CompletableFuture.delayedExecutor(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                .execute(() -> {
                    String response = AiRouter.reply("有人在游戏里对你说：" + message + "\n请简短回复。");
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.execute(() -> {
                        ClientPlayerEntity player = client.player;
                        if (player != null && player.networkHandler != null) {
                            player.networkHandler.sendChatMessage(response);
                            AiBotLogger.action("回复聊天: " + response.substring(0, Math.min(50, response.length())));
                        }
                        isThinking = false;
                    });
                });
    }

    private static boolean trySetFloat(Object obj, String fieldName, float value) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.setFloat(obj, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean trySetBoolean(Object obj, String fieldName, boolean value) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.setBoolean(obj, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
