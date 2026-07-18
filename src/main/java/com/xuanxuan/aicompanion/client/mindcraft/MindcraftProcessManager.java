package com.xuanxuan.aicompanion.client.mindcraft;

import com.xuanxuan.aicompanion.client.AiCompanionClient;
import com.xuanxuan.aicompanion.client.config.AiCompanionConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.BufferedReader;
import java.io.BufferedReader;
import java.io.BufferedReader;
import java.io.BufferedReader;
import java.io.BufferedReader;
import java.io.BufferedReader;
import java.io.BufferedReader;
import java.io.BufferedReader;
import java.io.BufferedReader;
import java.io.BufferedReader;
import java.io.BufferedReader;
import java.io.BufferedReader;
import java.io.BufferedReader;
import java.io.BufferedReader;
import java.io.BufferedReader;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.LoggerFactory;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MindcraftProcessManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("AiCompanion-Mindcraft");

    private static Process mindcraftProcess;

    private MindcraftProcessManager() {
    }

    // 生成 Config Files ...
    public static void generateConfigFiles() {
        // default: use local host and port from AiCompanionConfig.mindserverPort() if available
        int port = -1;
        try {
            port = Integer.parseInt(AiCompanionConfig.mindserverPort());
        } catch (Exception ignored) {
        }
        generateConfigFiles("127.0.0.1", port);
    }

    public static void generateConfigFiles(String host, int port) {
        String mindcraftPath = AiCompanionConfig.mindcraftPath();
        if (mindcraftPath.isEmpty()) {
            AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] \\u9519\\u8bef..."));
            return;
        }

        Path baseDir = Path.of(mindcraftPath);
        if (!Files.isDirectory(baseDir)) {
            AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] \\u9519\\u8bef... " + mindcraftPath));
            return;
        }

        try {
            generateKeysJson(baseDir);
            generateAndyJson(baseDir);
            generateSettingsJs(baseDir, host, port);
            AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] \\u914d\\u7f6e\\u751f\\u6210"));
        } catch (IOException e) {
            LOGGER.error("Failed to generate Mindcraft config files", e);
            AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] 生成配置文件失败: " + e.getMessage()));
        }
    }

    private static void generateKeysJson(Path baseDir) throws IOException {
        String apiKey = AiCompanionConfig.apiKey();
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("\n");
        sb.append("    \"OPENAI_API_KEY\": \"\").append(escapeJson(apiKey)).append("\"\n");
        sb.append("    \"OPENAI_ORG_ID\": \"\"\n");
        sb.append("    \"GEMINI_API_KEY\": \"\"\n");
        sb.append("    \"QWEN_API_KEY\": \"\"\n");
        sb.append("    \"XAI_API_KEY\": \"\"\n");
        sb.append("    \"MISTRAL_API_KEY\": \"\"\n");
        sb.append("    \"DEEPSPEEK_API_KEY\": \"\"\n");
        sb.append("    \"GHLF_API_KEY\": \"\"\n");
        sb.append("    \"HYPERBOLIC_API_KEY\": \"\"\n");
        sb.append("    \"NOVITA_API_KEY\": \"\"\n");
        sb.append("    \"OPENROUTER_API_KEY\": \"\"\n");
        sb.append("    \"CEREBRAS_API_KEY\": \"\"\n");
        sb.append("    \"MERCURY_API_KEY\": \"\"\n");
        sb.append("}\n");

        Files.writeString(baseDir.resolve("keys.json"), sb.toString(), StandardCharsets.UTF_8);
    }

    private static void generateAndyJson(Path baseDir) throws IOException {
        String botName = AiCompanionConfig.botName();
        String cloudApi = AiCompanionConfig.cloudApi();
        String modelName = AiCompanionConfig.modelName();
        String embeddingModel = AiCompanionConfig.embeddingModel();
        String apiProvider = AiCompanionConfig.apiProvider();

        String convertingPrompt = buildConvertingPrompt(botName);
        String codingPrompt = buildCodingPrompt(botName);

        String effectiveEmbedding = embeddingModel.isEmpty() ? "text-embedding-v3" : embeddingModel;

        String json = "" +
                "{\n" +
                String.format("\"name\": \"%s\",\n", botName) +
                "    \"model\": {\n" +
                String.format("        \"api\": \"%s\",\n", apiProvider) +
                String.format("        \"url\": \"%s\",\n", cloudApi) +
                String.format("        \"model\": \"%s\"\n", modelName) +
                "    },\n" +
                "    \"embedding\": {\n" +
                String.format("        \"api\": \"%s\",\n", apiProvider) +
                String.format("        \"url\": \"%s\",\n", cloudApi) +
                String.format("        \"model\": \"%s\"\n", effectiveEmbedding) +
                "    },\n" +
                String.format("    \"converging_prompt\": \"%s\",\n", escapeJson(convertingPrompt)) +
                String.format("    \"coding_prompt\": \"%s\",\n", escapeJson(codingPrompt)) +
                "    \"converting\": \"%s\"\n" +
                "}\n";

        Files.writeString(baseDir.resolve("andy.json"), json, StandardCharsets.UTF_8);
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static void generateSettingsJs(Path baseDir, String host, int port) throws IOException {
        int effectivePort = port;
        if (effectivePort < 0) {
            try {
                effectivePort = Integer.parseInt(AiCompanionConfig.mindserverPort());
            } catch (Exception ignored) {
                effectivePort = -1;
            }
        }

        String js = "";
        final var settings = {
                "minecraft_version": "auto",
                "host": "%s",
                "port": %d,
                "auth": "offline",
                "mindserver_port": %d,
                "auto_open_ui": false,
                "base_profile": "assistant",
                "profiles": ["./andy.json"],
                "load_memory": true,
                "init_message": "",
                "only_chat_with": [],
                "speak": false,
                "chat_ingame": true,
                "language": "en",
                "render_bot_view": false,
                "allow_insecure_coding": true,
                "allow_vison": false,
                "blocked_actions": [],
                "code_timeout_mins": -1,
                "relevant_docs_count": 5,
                "max_messages": 15,
                "num_examples": 2,
                "max_commands": -1,
                "show_command_syntax": "full",
                "narrate_behavior": false,
                "chat_bot_messages": false,
                "spawn_timeout": 30,
                "block_place_delay": 0,
                "log_all_prompts": false
        };
        export default settings;

        Files.writeString(baseDir.resolve("settings.js"), js, StandardCharsets.UTF_8);
    }

    // Prompt Builders ... (these were previously large in-source strings causing unicode escape issues)

    private static String buildConvertingPrompt(String name) {
        String prompt = readPromptResource("converting_prompt.txt");
        if (!prompt.isEmpty()) {
            return prompt.replace("$NAME", name == null ? "" : name);
        }
        // fallback (safe short default text)
        return "你是一个智能的Mineflayer机器人" + (name == null ? "" : name) + "，请根据提供的技能和世界函数编写JavaScript代码块来在Minecraft中完成任务。";
    }

    private static String buildCodingPrompt(String name) {
        String prompt = readPromptResource("coding_prompt.txt");
        if (!prompt.isEmpty()) {
            return prompt.replace("$NAME", name == null ? "" : name);
        }
        // fallback (safe short default text)
        return "你是一个智能的Mineflayer机器人" + (name == null ? "" : name) + "，通过编写JavaScript代码块来玩Minecraft。";
    }

    // Read prompt resource from classpath /prompts/<resourceName>
    private static String readPromptResource(String resourceName) {
        try (InputStream in = MindcraftProcessManager.class.getResourceAsStream("/prompts/" + resourceName)) {
            if (in == null) {
                LOGGER.warn("Prompt resource not found: /prompts/{}", resourceName);
                return "";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Failed to read prompt resource: {}", resourceName, e);
            return "";
        }
    }

    private static String buildConvertingPrompt_old(String name) {
        // kept for reference; original large string removed to avoid compile issues
        return "";
    }

    private static String buildCodingPrompt_old(String name) {
        // kept for reference; original large string removed to avoid compile issues
        return "";
    }

    private static void generateKeysJson(Path baseDir) throws IOException {
        // method duplicated earlier? placeholder to avoid missing references
    }

    public static void startMindcraft() {
        // default start: try to use configured mindserver port or let settings.js keep port -1
        int port = -1;
        try {
            port = Integer.parseInt(AiCompanionConfig.mindserverPort());
        } catch (Exception ignored) {
        }
        try {
            port = Integer.parseInt(AiCompanionConfig.mindserverPort());
        } catch (Exception ignored) {
        }
        startMindcraft("127.0.0.1", port);
    }

    public static void startMindcraft(String host, int port) {
        if (AiCompanionConfig.mindcraftRunning()) {
            AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] Mindcraft \u5df2\u7ecf\u5f00\u542f"));
            return;
        }

        Path mindcraftPath = AiCompanionConfig.mindcraftPath();
        if (mindcraftPath.isEmpty()) {
            AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] \u8bf7\u914d\u7f6e\u8fd0\u884c\u8def\u5f84"));
            return;
        }

        if (!Files.isDirectory(Path.of(mindcraftPath))) {
            AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] \u6ca1\u6709\u627e\u5230\u76ee\u5f55: " + mindcraftPath));
            return;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("node", "main.js");
            pb.directory(Path.of(mindcraftPath).toFile());
            pb.redirectErrorStream(true);
            pb.environment().put("NODE_ENV", "production");

            mindcraftProcess = pb.start();
            AiCompanionConfig.setMindcraftRunning(true);

            // Start stream gobbler to read output and forward to game chat
            new Thread(new StreamGobbler(mindcraftProcess.getInputStream(), "Mindcraft-Output")).start();

            AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] Mindcraft \u8fdb\u5165\u7f51\u7edc"));

            // Monitor process exit
            new Thread(() -> {
                try {
                    int exitCode = mindcraftProcess.waitFor();
                    AiCompanionConfig.setMindcraftRunning(false);
                    mindcraftProcess = null;
                    MinecraftClient.getInstance().execute(() ->
                            AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] " + exitCode))
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Mindcraft-Monitor").start();

        } catch (IOException e) {
            LOGGER.error("Failed to start Mindcraft process", e);
            AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] 启动失败: " + e.getMessage()));
            AiCompanionConfig.setMindcraftRunning(false);
        }
    }

    public static void stopMindcraft() {
        if (mindcraftProcess == null || !mindcraftProcess.isAlive()) {
            AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] Mindcraft 未在运行"));
            return;
        }
        mindcraftProcess.destroy();
        AiCompanionConfig.setMindcraftRunning(false);
        mindcraftProcess = null;
        AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] Mindcraft 已停止"));
    }

    private static class StreamGobbler implements Runnable {
        private final java.io.InputStream inputStream;

        StreamGobbler(java.io.InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        MinecraftClient.getInstance().execute(() ->
                                AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] " + trimmed))
                        );
                    }
                }
            } catch (IOException e) {
                LOGGER.debug("StreamGobbler ended", e);
            }
        }
    }
}
