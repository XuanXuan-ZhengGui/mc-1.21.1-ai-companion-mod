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
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public final class AiBotController {
    private static final int DECISION_INTERVAL = 40;
    private static final Random RANDOM = new Random();
    private static boolean initialized = false;
    private static int tickCounter = 0;
    private static int decisionCooldown = 0;
    private static AiBotInput currentInput = new AiBotInput();
    private static boolean isThinking = false;
    private static boolean inputReflectionWorks = true;
    private static boolean isFleeing = false;
    private static int fleeTicks = 0;

    private AiBotController() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (AiCompanionConfig.aiBotMode()) {
                AiCompanionClient.addChatMessage(Text.literal("[AI Bot] 已加入世界，AI 控制启动中..."));
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

        decisionCooldown--;
        if (decisionCooldown <= 0 && !isThinking) {
            requestDecision(client);
            decisionCooldown = DECISION_INTERVAL + RANDOM.nextInt(20);
        }

        applyInput(client);
        executeActions(client);
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
                currentInput.reset();
                currentInput.forward = -1.0f;
                currentInput.sideways = RANDOM.nextFloat() * 2.0f - 1.0f;
                currentInput.jumping = true;
                AiCompanionClient.addChatMessage(Text.literal("[AI Bot] 正在逃跑！"));
            }
        }

        if (isFleeing) {
            fleeTicks--;
            if (fleeTicks <= 0 || (health >= 12.0f && !inDanger)) {
                isFleeing = false;
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
        String prompt = "你是 Minecraft 中的 AI 玩家 " + AiPerception.BOT_NAME + "。\n"
                + "目标: 生存、探索、协助其他玩家。\n"
                + "当前环境:\n" + perception
                + "\n请决定下一步动作组合。格式（每行一个）:\n"
                + "移动: 前进|后退|左|右|停止\n"
                + "跳跃: 是|否\n"
                + "蹲下: 是|否\n"
                + "攻击: 是|否\n"
                + "使用物品: 是|否\n"
                + "聊天: 要说的话（可选）";

        CompletableFuture.supplyAsync(() -> AiRouter.reply(prompt))
                .thenAccept(response -> client.execute(() -> {
                    parseDecision(response);
                    isThinking = false;
                }));
    }

    private static void parseDecision(String response) {
        currentInput.reset();
        if (response == null || response.isBlank()) return;

        String lower = response.toLowerCase();

        if (lower.contains("移动:前进") || lower.contains("前进")) currentInput.forward = 1.0f;
        else if (lower.contains("移动:后退") || lower.contains("后退")) currentInput.forward = -1.0f;
        else if (lower.contains("移动:左") || lower.contains("向左")) currentInput.sideways = 1.0f;
        else if (lower.contains("移动:右") || lower.contains("向右")) currentInput.sideways = -1.0f;

        if (lower.contains("跳跃:是") || lower.contains("跳跃")) currentInput.jumping = true;
        if (lower.contains("蹲下:是") || lower.contains("蹲下")) currentInput.sneaking = true;
        if (lower.contains("攻击:是") || lower.contains("攻击")) currentInput.attacking = true;
        if (lower.contains("使用物品:是") || lower.contains("使用")) currentInput.using = true;

        int chatIdx = response.indexOf("聊天:");
        if (chatIdx >= 0) {
            currentInput.chatMessage = response.substring(chatIdx + 3).trim();
            if (currentInput.chatMessage.length() > 100) {
                currentInput.chatMessage = currentInput.chatMessage.substring(0, 100);
            }
        }
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

    private static void executeActions(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        if (currentInput.chatMessage != null && !currentInput.chatMessage.isBlank()) {
            ClientPlayNetworkHandler handler = player.networkHandler;
            if (handler != null) {
                handler.sendChatMessage(currentInput.chatMessage);
                currentInput.chatMessage = null;
            }
        }

        if (currentInput.attacking) {
            Entity target = findNearestHostile(client);
            if (target != null) {
                faceEntity(player, target);
                if (client.interactionManager != null) {
                    client.interactionManager.attackEntity(player, target);
                }
            }
            currentInput.attacking = false;
        }

        if (currentInput.using) {
            if (client.interactionManager != null) {
                client.interactionManager.interactItem(player, Hand.MAIN_HAND);
            }
            currentInput.using = false;
        }
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
        CompletableFuture.delayedExecutor(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                .execute(() -> {
                    String response = AiRouter.reply("有人在游戏里对你说：" + message + "\n请简短回复。");
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.execute(() -> {
                        ClientPlayerEntity player = client.player;
                        if (player != null && player.networkHandler != null) {
                            player.networkHandler.sendChatMessage(response);
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
