package com.xuanxuan.aicompanion.client.bot;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public final class AiPerception {
    private AiPerception() {
    }

    public static String describe(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) {
            return "无法获取环境信息。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("位置: ").append(blockPosStr(player.getBlockPos())).append("\n");
        sb.append("生命值: ").append(String.format("%.1f", player.getHealth())).append("/").append(player.getMaxHealth()).append("\n");
        try {
            sb.append("饥饿值: ").append(player.getHungerManager().getFoodLevel()).append("/20\n");
        } catch (Exception ignored) {
        }
        sb.append("朝向: Yaw=").append(String.format("%.0f", player.getYaw())).append(", Pitch=").append(String.format("%.0f", player.getPitch())).append("\n");
        sb.append("在地面上: ").append(player.isOnGround()).append("\n");

        List<Entity> nearby = world.getEntities(player, player.getBoundingBox().expand(16.0));
        int hostileCount = 0;
        int playerCount = 0;
        int passiveCount = 0;
        StringBuilder nearest = new StringBuilder();
        double nearestDist = Double.MAX_VALUE;
        Entity nearestEntity = null;

        for (Entity e : nearby) {
            if (e == player) continue;
            double dist = player.squaredDistanceTo(e);
            if (e instanceof HostileEntity) hostileCount++;
            if (e instanceof PlayerEntity) playerCount++;
            if (e instanceof PassiveEntity) passiveCount++;
            if (dist < nearestDist) {
                nearestDist = dist;
                nearestEntity = e;
            }
        }

        sb.append("周围实体: 敌对=").append(hostileCount).append(", 玩家=").append(playerCount).append(", 友好=").append(passiveCount).append("\n");
        if (nearestEntity != null) {
            sb.append("最近实体: ").append(nearestEntity.getType().getTranslationKey())
                    .append(" 距离=").append(String.format("%.1f", Math.sqrt(nearestDist))).append("\n");
        }

        long time = world.getTimeOfDay() % 24000;
        sb.append("时间: ").append(time > 12000 ? "夜晚" : "白天").append("\n");

        return sb.toString();
    }

    private static String blockPosStr(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }
}
