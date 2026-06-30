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

    private AiCompanionConfig() {
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            return;
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(CONFIG_PATH)) {
            properties.load(inputStream);
            providerMode = ProviderMode.valueOf(properties.getProperty("providerMode", ProviderMode.CLOUD.name()));
            cloudApi = properties.getProperty("cloudApi", "");
            modelName = properties.getProperty("modelName", "");
            chatMode = Boolean.parseBoolean(properties.getProperty("chatMode", "true"));
            joinedGame = Boolean.parseBoolean(properties.getProperty("joinedGame", "false"));
            aiBotMode = Boolean.parseBoolean(properties.getProperty("aiBotMode", "false"));
            targetServerIp = properties.getProperty("targetServerIp", "");
        } catch (IOException | IllegalArgumentException ignored) {
            providerMode = ProviderMode.CLOUD;
        }
    }

    public static void save() {
        Properties properties = new Properties();
        properties.setProperty("providerMode", providerMode.name());
        properties.setProperty("cloudApi", cloudApi);
        properties.setProperty("modelName", modelName);
        properties.setProperty("chatMode", Boolean.toString(chatMode));
        properties.setProperty("joinedGame", Boolean.toString(joinedGame));
        properties.setProperty("aiBotMode", Boolean.toString(aiBotMode));
        properties.setProperty("targetServerIp", targetServerIp);

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (OutputStream outputStream = Files.newOutputStream(CONFIG_PATH)) {
                properties.store(outputStream, "AI Companion configuration");
            }
        } catch (IOException ignored) {
        }
    }

    public static ProviderMode providerMode() {
        return providerMode;
    }

    public static void setProviderMode(ProviderMode providerMode) {
        AiCompanionConfig.providerMode = providerMode;
    }

    public static String cloudApi() {
        return cloudApi;
    }

    public static void setCloudApi(String cloudApi) {
        AiCompanionConfig.cloudApi = cloudApi == null ? "" : cloudApi.trim();
    }

    public static String modelName() {
        return modelName;
    }

    public static void setModelName(String modelName) {
        AiCompanionConfig.modelName = modelName == null ? "" : modelName.trim();
    }

    public static boolean chatMode() {
        return chatMode;
    }

    public static void setChatMode(boolean chatMode) {
        AiCompanionConfig.chatMode = chatMode;
    }

    public static boolean joinedGame() {
        return joinedGame;
    }

    public static void setJoinedGame(boolean joinedGame) {
        AiCompanionConfig.joinedGame = joinedGame;
    }

    public static boolean aiBotMode() {
        return aiBotMode;
    }

    public static void setAiBotMode(boolean aiBotMode) {
        AiCompanionConfig.aiBotMode = aiBotMode;
    }

    public static String targetServerIp() {
        return targetServerIp;
    }

    public static void setTargetServerIp(String targetServerIp) {
        AiCompanionConfig.targetServerIp = targetServerIp == null ? "" : targetServerIp.trim();
    }

    public static String localEndpoint() {
        return providerMode == ProviderMode.LOCAL_LLAMA_CPP
                ? "http://127.0.0.1:8080/completion"
                : "http://127.0.0.1:11434/api/generate";
    }

    public static String providerDisplayName() {
        return switch (providerMode) {
            case CLOUD -> "云端";
            case LOCAL_OLLAMA -> "本地 ollama";
            case LOCAL_LLAMA_CPP -> "本地 llama.cpp";
        };
    }
}
