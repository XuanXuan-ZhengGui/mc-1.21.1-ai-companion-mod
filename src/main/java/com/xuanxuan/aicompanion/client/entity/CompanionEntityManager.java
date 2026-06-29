package com.xuanxuan.aicompanion.client.entity;

import com.mojang.authlib.GameProfile;
import com.xuanxuan.aicompanion.client.AiCompanionClient;
import com.xuanxuan.aicompanion.client.ai.AiRouter;
import com.xuanxuan.aicompanion.client.config.AiCompanionConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class CompanionEntityManager {
    public static final String COMPANION_NAME = "XuanXuan-ZhengGui";

    private static OtherClientPlayerEntity companion;
    private static UUID skinUuid = UUID.randomUUID();
    private static int tickCounter;
    private static int spawnDelayTicks;
    private static int deathCooldownTicks;
    private static final Random RANDOM = new Random();
    private static boolean isThinking = false;

    private CompanionEntityManager() {
    }

    public static void tick(MinecraftClient client) {
        try {
            if (client.world == null || client.player == null || !AiCompanionConfig.joinedGame()) {
                remove(client);
                spawnDelayTicks = 0;
                deathCooldownTicks = 0;
                return;
            }

            if (deathCooldownTicks > 0) {
                deathCooldownTicks--;
                return;
            }

            if (companion == null || companion.isRemoved() || companion.getWorld() != client.world) {
                spawnDelayTicks++;
                if (spawnDelayTicks > 20) {
                    spawn(client);
                    spawnDelayTicks = 0;
                }
                return;
            }

            tickCounter++;
            updateSurvival(client);
            updateDamage(client);

            if (tickCounter % 5 == 0) {
                updateMovement(client);
            }
            if (tickCounter % 20 == 0) {
                updateBehavior(client);
            }
        } catch (Exception exception) {
            AiCompanionClient.addChatMessage(Text.literal("[AI Companion] 实体更新出错：" + exception.getMessage()));
        }
    }

    public static void respawn(MinecraftClient client) {
        skinUuid = UUID.randomUUID();
        remove(client);
        spawnDelayTicks = 0;
        deathCooldownTicks = 0;
    }

    public static void remove(MinecraftClient client) {
        if (companion == null) {
            return;
        }
        try {
            if (client.world != null && !companion.isRemoved()) {
                client.world.removeEntity(companion.getId(), Entity.RemovalReason.DISCARDED);
            }
        } catch (Exception ignored) {
        }
        companion = null;
    }

    public static boolean isCompanionActive() {
        return companion != null && !companion.isRemoved();
    }

    public static void onPlayerChat(String message) {
        if (!isCompanionActive() || isThinking) {
            return;
        }

        String lowerMsg = message.toLowerCase();
        String modelName = AiCompanionConfig.modelName().toLowerCase();
        String companionName = COMPANION_NAME.toLowerCase();

        boolean shouldReply = lowerMsg.contains("@" + modelName)
                || lowerMsg.contains("@*" + modelName)
                || lowerMsg.contains(companionName)
                || lowerMsg.contains("玄玄")
                || lowerMsg.contains("ai");

        if (!shouldReply) {
            return;
        }

        long delay = 800 + RANDOM.nextInt(1500);
        isThinking = true;

        CompletableFuture.delayedExecutor(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                .execute(() -> {
                    try {
                        String response = AiRouter.reply(message);
                        MinecraftClient client = MinecraftClient.getInstance();
                        client.execute(() -> {
                            AiCompanionClient.addChatMessage(
                                    Text.literal("[" + COMPANION_NAME + "] " + response)
                            );
                            isThinking = false;
                        });
                    } catch (Exception exception) {
                        isThinking = false;
                    }
                });
    }

    private static void spawn(MinecraftClient client) {
        try {
            if (client.world == null || client.player == null) return;

            GameProfile profile = new GameProfile(skinUuid, COMPANION_NAME);
            companion = new OtherClientPlayerEntity(client.world, profile);
            Vec3d spawnPos = companionPosition(client, 2.0D);
            companion.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, client.player.getYaw(), 0.0F);
            client.world.addEntity(companion);
            companion.setHealth(20.0f);
        } catch (Exception exception) {
            AiCompanionClient.addChatMessage(Text.literal("[AI Companion] 生成实体失败：" + exception.getMessage()));
            companion = null;
        }
    }

    private static void updateSurvival(MinecraftClient client) {
        if (companion == null || client.player == null) return;
        try {
            companion.setAir(companion.getMaxAir());
        } catch (Exception ignored) {
        }
    }

    private static void updateDamage(MinecraftClient client) {
        if (companion == null || companion.getHealth() <= 0) return;
        try {
            if (companion.fallDistance > 3.0f) {
                float damage = (companion.fallDistance - 3.0f) * 0.5f;
                companion.setHealth(Math.max(0, companion.getHealth() - damage));
                companion.fallDistance = 0.0f;
            }

            if (companion.getHealth() <= 0) {
                onDeath(client);
            }
        } catch (Exception ignored) {
        }
    }

    private static void onDeath(MinecraftClient client) {
        AiCompanionClient.addChatMessage(Text.literal("[" + COMPANION_NAME + "] 已死亡，3秒后重生..."));
        remove(client);
        deathCooldownTicks = 60;
    }

    private static void updateMovement(MinecraftClient client) {
        if (companion == null || client.player == null) {
            return;
        }
        try {
            double squaredDistance = companion.squaredDistanceTo(client.player);

            if (squaredDistance > 400.0D) {
                Vec3d position = companionPosition(client, 2.0D);
                companion.refreshPositionAndAngles(position.x, position.y, position.z, client.player.getYaw(), 0.0F);
                companion.setVelocity(0, 0, 0);
                return;
            }

            if (squaredDistance > 9.0D) {
                Vec3d target = companionPosition(client, 2.5D);
                Vec3d diff = target.subtract(companion.getPos());
                Vec3d movement = diff.multiply(0.12D);

                if (companion.isOnGround() && shouldJump(client, diff)) {
                    movement = new Vec3d(movement.x, 0.42D, movement.z);
                } else {
                    movement = new Vec3d(movement.x, companion.getVelocity().y, movement.z);
                }

                companion.setVelocity(movement);

                float yaw = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
                companion.setYaw(yaw);
                companion.setHeadYaw(yaw);
            } else {
                Vec3d vel = companion.getVelocity();
                companion.setVelocity(vel.x * 0.5, vel.y, vel.z * 0.5);

                if (RANDOM.nextInt(15) == 0) {
                    facePlayer(client);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean shouldJump(MinecraftClient client, Vec3d diff) {
        return diff.y > 0.5;
    }

    private static void updateBehavior(MinecraftClient client) {
        if (companion == null || client.player == null) return;
        try {
            if (RANDOM.nextInt(8) == 0) {
                facePlayer(client);
            }
        } catch (Exception ignored) {
        }
    }

    private static void facePlayer(MinecraftClient client) {
        if (companion == null || client.player == null) return;
        try {
            double dx = client.player.getX() - companion.getX();
            double dz = client.player.getZ() - companion.getZ();
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            companion.setYaw(yaw);
            companion.setHeadYaw(yaw);
        } catch (Exception ignored) {
        }
    }

    private static Vec3d companionPosition(MinecraftClient client, double distance) {
        float yawRadians = client.player.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        double offsetX = -MathHelper.sin(yawRadians) * distance;
        double offsetZ = MathHelper.cos(yawRadians) * distance;
        return client.player.getPos().add(offsetX, 0.0D, offsetZ);
    }
}
