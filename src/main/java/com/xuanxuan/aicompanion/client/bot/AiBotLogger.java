package com.xuanxuan.aicompanion.client.bot;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class AiBotLogger {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static boolean enabled = true;

    private AiBotLogger() {
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void log(String category, String message) {
        if (!enabled) return;
        String time = LocalTime.now().format(TIME_FORMAT);
        Text text = Text.literal("")
                .append(Text.literal("[" + time + "] ").formatted(Formatting.GRAY))
                .append(Text.literal("[" + category + "] ").formatted(Formatting.AQUA))
                .append(Text.literal(message).formatted(Formatting.WHITE));
        send(text);
    }

    public static void think(String message) {
        log("思考", message);
    }

    public static void action(String message) {
        log("动作", message);
    }

    public static void perceive(String message) {
        log("感知", message);
    }

    public static void warn(String message) {
        log("警告", message);
    }

    public static void info(String message) {
        log("信息", message);
    }

    private static void send(Text text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(text);
        }
    }
}
