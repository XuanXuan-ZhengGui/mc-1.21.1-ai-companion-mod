package com.xuanxuan.aicompanion.client.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class AiCompanionConfig {
    public enum ProviderMode {
        CLOUD,
        LOCAL_OLLAMA,
        LOCAL_LLAMA_CPP
    }

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("ai-companion.properties");

    private static ProviderMode providerMode = ProviderMode.CLOUD;
    private static String cloudApi = "";
    private static String modelName = "";
    private static boolean chatMode = true;
    private static boolean joinedGame = false;
    private static boolean aiBotMode = false;
    private static String targetServerIp = "";
    private static String apiKey = "";
    private static String botName = "Andy";
    private static String mindcraftPath = "";
    private static boolean mindcraftRunning = false;
    private static String mindserverPort = "8080";
    private static String apiProvider = "openai";
    private static String embeddingModel = "";

    private AiCompanionConfig() {}

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) return;
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
            properties.load(in);
            providerMode = ProviderMode.valueOf(properties.getProperty("providerMode", ProviderMode.CLOUD.name()));
            cloudApi = properties.getProperty("cloudApi", "");
            modelName = properties.getProperty("modelName", "");
            chatMode = Boolean.parseBoolean(properties.getProperty("chatMode", "true"));
            joinedGame = Boolean.parseBoolean(properties.getProperty("joinedGame", "false"));
            aiBotMode = Boolean.parseBoolean(properties.getProperty("aiBotMode", "false"));
            targetServerIp = properties.getProperty("targetServerIp", "");
            apiKey = properties.getProperty("apiKey", "");
            botName = properties.getProperty("botName", "Andy");
            mindcraftPath = properties.getProperty("mindcraftPath", "");
            mindserverPort = properties.getProperty("mindserverPort", "8080");
            apiProvider = properties.getProperty("apiProvider", "openai");
            embeddingModel = properties.getProperty("embeddingModel", "");
        } catch (IOException | IllegalArgumentException ignored) {
            providerMode = ProviderMode.CLOUD;
        }
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("providerMode", providerMode.name());
        p.setProperty("cloudApi", cloudApi);
        p.setProperty("modelName", modelName);
        p.setProperty("chatMode", Boolean.toString(chatMode));
        p.setProperty("joinedGame", Boolean.toString(joinedGame));
        p.setProperty("aiBotMode", Boolean.toString(aiBotMode));
        p.setProperty("targetServerIp", targetServerIp);
        p.setProperty("apiKey", apiKey);
        p.setProperty("botName", botName);
        p.setProperty("mindcraftPath", mindcraftPath);
        p.setProperty("mindserverPort", mindserverPort);
        p.setProperty("apiProvider", apiProvider);
        p.setProperty("embeddingModel", embeddingModel);
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
                p.store(out, "AI Companion configuration");
            }
        } catch (IOException ignored) {}
    }

    // ===== existing getters/setters =====

    public static ProviderMode providerMode() { return providerMode; }
    public static void setProviderMode(ProviderMode v) { providerMode = v; }

    public static String cloudApi() { return cloudApi; }
    public static void setCloudApi(String v) { cloudApi = v == null ? "" : v.trim(); }

    public static String modelName() { return modelName; }
    public static void setModelName(String v) { modelName = v == null ? "" : v.trim(); }

    public static boolean chatMode() { return chatMode; }
    public static void setChatMode(boolean v) { chatMode = v; }

    public static boolean joinedGame() { return joinedGame; }
    public static void setJoinedGame(boolean v) { joinedGame = v; }

    public static boolean aiBotMode() { return aiBotMode; }
    public static void setAiBotMode(boolean v) { aiBotMode = v; }

    public static String targetServerIp() { return targetServerIp; }
    public static void setTargetServerIp(String v) { targetServerIp = v == null ? "" : v.trim(); }

    public static String localEndpoint() {
        return providerMode == ProviderMode.LOCAL_LLAMA_CPP
                ? "http://127.0.0.1:8080/completion"
                : "http://127.0.0.1:11434/api/generate";
    }

    public static String providerDisplayName() {
        return switch (providerMode) {
            case CLOUD -> "\u4e91\u7aef";
            case LOCAL_OLLAMA -> "\u672c\u5730 ollama";
            case LOCAL_LLAMA_CPP -> "\u672c\u5730 llama.cpp";
        };
    }

    // ===== new mindcraft getters/setters =====

    public static String apiKey() { return apiKey; }
    public static void setApiKey(String v) { apiKey = v == null ? "" : v.trim(); }

    public static String botName() { return botName; }
    public static void setBotName(String v) { botName = v == null ? "Andy" : v.trim(); }

    public static String mindcraftPath() { return mindcraftPath; }
    public static void setMindcraftPath(String v) { mindcraftPath = v == null ? "" : v.trim(); }

    public static boolean mindcraftRunning() { return mindcraftRunning; }
    public static void setMindcraftRunning(boolean v) { mindcraftRunning = v; }

    public static String mindserverPort() { return mindserverPort; }
    public static void setMindserverPort(String v) { mindserverPort = v == null ? "8080" : v.trim(); }

    public static String apiProvider() { return apiProvider; }
    public static void setApiProvider(String v) { apiProvider = v == null ? "openai" : v.trim(); }

    public static String embeddingModel() { return embeddingModel; }
    public static void setEmbeddingModel(String v) { embeddingModel = v == null ? "" : v.trim(); }
}
