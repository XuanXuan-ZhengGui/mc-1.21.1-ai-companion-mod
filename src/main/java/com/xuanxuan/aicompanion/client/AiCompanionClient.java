package com.xuanxuan.aicompanion.client;

import com.xuanxuan.aicompanion.client.ai.AiRouter;
import com.xuanxuan.aicompanion.client.bot.AiBotController;
import com.xuanxuan.aicompanion.client.config.AiCompanionConfig;
import com.xuanxuan.aicompanion.client.mindcraft.MindcraftProcessManager;
import com.xuanxuan.aicompanion.client.entity.CompanionEntityManager;
import com.xuanxuan.aicompanion.client.gui.LoadedMapScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.CompletableFuture;

public final class AiCompanionClient implements ClientModInitializer {
    public static final String MOD_ID = "ai_companion";

    private static KeyBinding openMapKey;
    private static boolean lanOpened = false;
    private static int lanDelayTicks = 0;

    @Override
    public void onInitializeClient() {
        AiCompanionConfig.load();
        AiBotController.init();

        Runtime.getRuntime().addShutdownHook(new Thread(MindcraftProcessManager::stopMindcraft, "Mindcraft-Shutdown"));

        openMapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ai_companion.open_map",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "category.ai_companion"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMapKey.wasPressed()) {
                client.setScreen(new LoadedMapScreen());
            }
            CompanionEntityManager.tick(client);
            tryOpenLan(client);
            tryStartMindcraftForCurrentServer(client);
        });

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (!shouldHandleAiMention(message)) {
                if (AiCompanionConfig.joinedGame()) {
                    CompanionEntityManager.onPlayerChat(message);
                }
                return true;
            }

            askAi(message);
            return false;
        });
    }

    private static void tryOpenLan(MinecraftClient client) {
        if (lanOpened) return;
        if (client.getServer() == null) return;
        if (client.player == null) return;
        if (client.world == null) return;
        if (!AiCompanionConfig.joinedGame()) return;

        lanDelayTicks++;
        if (lanDelayTicks < 40) return;

        try {
            boolean success = client.getServer().openToLan(GameMode.SURVIVAL, true, 0);
            if (success) {
                lanOpened = true;
                addChatMessage(Text.literal("[系统] 局域网世界已开启，其他玩家可以搜索并加入！"));

                // If AI Bot mode is enabled, try to start Mindcraft and pass host/port so the external bot can join the LAN server
                if (AiCompanionConfig.aiBotMode()) {
                    String host = detectLanAddress();
                    int port = -1;

                    try {
                        Object integratedServer = client.getServer();
                        if (integratedServer != null) {
                            // Try common getter names first
                            try {
                                Method m = integratedServer.getClass().getMethod("getPort");
                                Object p = m.invoke(integratedServer);
                                if (p instanceof Integer) port = (Integer) p;
                            } catch (NoSuchMethodException ignored) {
                                try {
                                    Method m2 = integratedServer.getClass().getMethod("getServerPort");
                                    Object p2 = m2.invoke(integratedServer);
                                    if (p2 instanceof Integer) port = (Integer) p2;
                                } catch (Exception ignored2) {
                                    // fallback to reflection field
                                }
                            } catch (Exception ignored) {
                                // ignore
                            }

                            if (port < 0) {
                                try {
                                    Field f = integratedServer.getClass().getDeclaredField("port");
                                    f.setAccessible(true);
                                    Object v = f.get(integratedServer);
                                    if (v instanceof Integer) port = (Integer) v;
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }

                    // fallback to configured mindserverPort or -1
                    if (port < 0) {
                        try {
                            port = Integer.parseInt(AiCompanionConfig.mindserverPort());
                        } catch (Exception ignored) {
                            port = -1;
                        }
                    }

                    MindcraftProcessManager.startMindcraft(host, port);
                }
            }
        } catch (Exception exception) {
            addChatMessage(Text.literal("[系统] 开启局域网失败：" + exception.getMessage()));
        }
    }

    // Try to start Mindcraft to join the server the player is currently in (supports targetServerIp, integrated LAN, or remote server)
    private static void tryStartMindcraftForCurrentServer(MinecraftClient client) {
        if (!AiCompanionConfig.aiBotMode()) return;
        if (MindcraftProcessManager.isRunning()) return;
        if (!AiCompanionConfig.joinedGame()) return;

        // 1) If user specified a target server (supports domain/IP or host:port), use that
        String target = AiCompanionConfig.targetServerIp();
        if (target != null && !target.isBlank()) {
            String host = target;
            int port = -1;
            if (target.contains(":")) {
                String[] parts = target.split(":", 2);
                host = parts[0];
                try { port = Integer.parseInt(parts[1]); } catch (Exception ignored) { port = -1; }
            } else {
                try { port = Integer.parseInt(AiCompanionConfig.mindserverPort()); } catch (Exception ignored) { port = -1; }
            }
            MindcraftProcessManager.startMindcraft(host, port);
            return;
        }

        // 2) If we're running an integrated server (singleplayer), use LAN-detect host + server port
        try {
            if (client.getServer() != null) {
                String host = detectLanAddress();
                int port = -1;

                try {
                    Object integratedServer = client.getServer();
                    if (integratedServer != null) {
                        try {
                            Method m = integratedServer.getClass().getMethod("getPort");
                            Object p = m.invoke(integratedServer);
                            if (p instanceof Integer) port = (Integer) p;
                        } catch (NoSuchMethodException ignored) {
                            try {
                                Method m2 = integratedServer.getClass().getMethod("getServerPort");
                                Object p2 = m2.invoke(integratedServer);
                                if (p2 instanceof Integer) port = (Integer) p2;
                            } catch (Exception ignored2) {
                            }
                        } catch (Exception ignored) {}

                        if (port < 0) {
                            try {
                                Field f = integratedServer.getClass().getDeclaredField("port");
                                f.setAccessible(true);
                                Object v = f.get(integratedServer);
                                if (v instanceof Integer) port = (Integer) v;
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception ignored) {}

                if (port < 0) {
                    try { port = Integer.parseInt(AiCompanionConfig.mindserverPort()); } catch (Exception ignored) { port = -1; }
                }

                MindcraftProcessManager.startMindcraft(host, port);
                return;
            }
        } catch (Exception ignored) {
        }

        // 3) Try to detect current remote server address (multiplayer) via various reflection points
        try {
            // Try client.getCurrentServerEntry().address or similar
            try {
                Method getEntry = client.getClass().getMethod("getCurrentServerEntry");
                Object entry = getEntry.invoke(client);
                if (entry != null) {
                    String address = null;
                    try {
                        Method addrM = entry.getClass().getMethod("address");
                        Object addr = addrM.invoke(entry);
                        if (addr instanceof String) address = (String) addr;
                    } catch (NoSuchMethodException ignored) {
                        try {
                            Method addrM2 = entry.getClass().getMethod("getAddress");
                            Object addr2 = addrM2.invoke(entry);
                            if (addr2 instanceof String) address = (String) addr2;
                        } catch (Exception ignored2) {}
                    } catch (Exception ignored) {}

                    if (address == null) {
                        try {
                            Field f = entry.getClass().getDeclaredField("address");
                            f.setAccessible(true);
                            Object v = f.get(entry);
                            if (v instanceof String) address = (String) v;
                        } catch (Exception ignored) {}
                    }

                    if (address != null && !address.isBlank()) {
                        String host = address;
                        int port = -1;
                        if (address.contains(":")) {
                            String[] parts = address.split(":", 2);
                            host = parts[0];
                            try { port = Integer.parseInt(parts[1]); } catch (Exception ignored) { port = -1; }
                        } else {
                            try { port = Integer.parseInt(AiCompanionConfig.mindserverPort()); } catch (Exception ignored) { port = -1; }
                        }
                        MindcraftProcessManager.startMindcraft(host, port);
                        return;
                    }
                }
            } catch (NoSuchMethodException ignored) {
                // continue to next approach
            }
        } catch (Exception ignored) {}

        // 4) As a last resort, try to extract remote address from network handler
        try {
            try {
                Method getNetworkHandler = client.getClass().getMethod("getNetworkHandler");
                Object nh = getNetworkHandler.invoke(client);
                if (nh != null) {
                    try {
                        Method getConnection = nh.getClass().getMethod("getConnection");
                        Object conn = getConnection.invoke(nh);
                        if (conn != null) {
                            try {
                                Method getAddress = conn.getClass().getMethod("getAddress");
                                Object addr = getAddress.invoke(conn);
                                String addrStr = addr != null ? addr.toString() : null;
                                if (addrStr != null && !addrStr.isBlank()) {
                                    // addrStr might be like /127.0.0.1:port or hostname/127.0.0.1:port
                                    String cleaned = addrStr;
                                    if (cleaned.startsWith("/")) cleaned = cleaned.substring(1);
                                    if (cleaned.contains("/")) cleaned = cleaned.substring(cleaned.lastIndexOf('/') + 1);
                                    if (cleaned.contains(":")) {
                                        String[] parts = cleaned.split(":" , 2);
                                        String host = parts[0];
                                        int port = -1;
                                        try { port = Integer.parseInt(parts[1]); } catch (Exception ignored) { port = -1; }
                                        MindcraftProcessManager.startMindcraft(host, port);
                                        return;
                                    }
                                }
                            } catch (NoSuchMethodException ignored) {}
                        }
                    } catch (NoSuchMethodException ignored) {}
                }
            } catch (NoSuchMethodException ignored) {}
        } catch (Exception ignored) {}

        // If nothing detected, do nothing — user can set targetServerIp in config (supports domain/public IP)
    }

    private static boolean shouldHandleAiMention(String message) {
        if (!AiCompanionConfig.chatMode() && !AiCompanionConfig.joinedGame()) {
            return false;
        }

        String modelName = AiCompanionConfig.modelName();
        if (modelName.isBlank()) {
            return false;
        }

        String trimmed = message.trim();
        return trimmed.startsWith("@" + modelName) || trimmed.startsWith("@*" + modelName);
    }

    private static void askAi(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        String modelName = AiCompanionConfig.modelName();
        String prompt = message
                .replaceFirst("^@\\*?" + java.util.regex.Pattern.quote(modelName), "")
                .trim();

        addChatMessage(Text.literal("[" + modelName + "] 正在思考..."));
        CompletableFuture
                .supplyAsync(() -> AiRouter.reply(prompt.isBlank() ? message : prompt))
                .thenAccept(answer -> client.execute(() -> addChatMessage(Text.literal("[" + modelName + "] " + answer))));
    }

    public static void addChatMessage(Text text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(text);
        }
    }

    private static String detectLanAddress() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                try {
                    if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) continue;
                } catch (Exception ignored) {
                }
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr.isLoopbackAddress()) continue;
                    if (addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();
                        if (!ip.startsWith("169.") && !ip.equals("0.0.0.0")) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "127.0.0.1";
    }
}
