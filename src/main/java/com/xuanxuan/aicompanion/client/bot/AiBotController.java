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
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;
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
        decisionCooldown--;

        if (decisionCooldown <= 0 && !isThinking) {
            requestDecision(client);
            decisionCooldown = DECISION_INTERVAL + RANDOM.nextInt(20);
        }

        applyInput(client);
        executeActions(client);
    }

    private static void requestDecision(MinecraftClient client) {
        isThinking = true;
        String perception = AiPerception.describe(client);
        String prompt = "你正在玩 Minecraft，是一个 AI 玩家。当前环境:\n" + perception
                + "\n请决定下一步动作。可用动作: 前进/后退/左/右/停止/跳跃/蹲下/攻击/使用物品/等待。"
                + "回复格式（每行一个）:\n移动:前进|后退|左|右|停止\n跳跃:是|否\n蹲下:是|否\n攻击:是|否\n使用:是|否\n聊天:内容（可选）";

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
        if (lower.contains("使用:是") || lower.contains("使用")) currentInput.using = true;

        int chatIdx = response.indexOf("聊天:");
        if (chatIdx >= 0) {
            currentInput.chatMessage = response.substring(chatIdx + 3).trim();
        }
    }

    private static void applyInput(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        try {
            Field inputField = ClientPlayerEntity.class.getDeclaredField("input");
            inputField.setAccessible(true);
            Object input = inputField.get(player);
            if (input == null) return;

            trySetFloat(input, "movementForward", currentInput.forward);
            trySetFloat(input, "movementSideways", currentInput.sideways);
            trySetBoolean(input, "jumping", currentInput.jumping);
            trySetBoolean(input, "sneaking", currentInput.sneaking);
        } catch (Exception ignored) {
        }
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

    private static void trySetFloat(Object obj, String fieldName, float value) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.setFloat(obj, value);
        } catch (Exception ignored) {
        }
    }

    private static void trySetBoolean(Object obj, String fieldName, boolean value) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.setBoolean(obj, value);
        } catch (Exception ignored) {
        }
    }
}
