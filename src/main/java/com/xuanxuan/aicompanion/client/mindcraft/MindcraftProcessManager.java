package com.xuanxuan.aicompanion.client.mindcraft;

import com.xuanxuan.aicompanion.client.AiCompanionClient;
import com.xuanxuan.aicompanion.client.config.AiCompanionConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MindcraftProcessManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("AiCompanion-Mindcraft");

    private static Process mindcraftProcess;

    private MindcraftProcessManager() {
    }

    // ── Generate Config Files ─────────────────────────────────────────────────────

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
            AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] \\u9519\\u8bef\\uff1a\\u672a\\u8bbe\\u7f6e Mindcraft \\u8def\\u5f84"));
            return;
        }

        Path baseDir = Path.of(mindcraftPath);
        if (!Files.isDirectory(baseDir)) {
            AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] \\u9519\\u8bef\\uff1a\\u8def\\u5f84\\u4e0d\\u5b58\\u5728\\u6216\\u4e0d\\u662f\\u76ee\\u5f55: " + mindcraftPath));
            return;
        }

        try {
            generateKeysJson(baseDir);
            generateAndyJson(baseDir);
            generateSettingsJs(baseDir, host, port);
            AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] \\u914d\\u7f6e\\u6587\\u4ef6\\u5df2\\u751f\\u6210"));
        } catch (IOException e) {
            LOGGER.error("Failed to generate Mindcraft config files", e);
            AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] \\u751f\\u6210\\u914d\\u7f6e\\u6587\\u4ef6\\u5931\\u8d25: " + e.getMessage()));
        }
    }

    private static void generateKeysJson(Path baseDir) throws IOException {
        String apiKey = AiCompanionConfig.apiKey();
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("    \"OPENAI_API_KEY\": \"").append(escapeJson(apiKey)).append("\",\n");
        sb.append("    \"OPENAI_ORG_ID\": \"\",\n");
        sb.append("    \"GEMINI_API_KEY\": \"").append(escapeJson(apiKey)).append("\",\n");
        sb.append("    \"ANTHROPIC_API_KEY\": \"").append(escapeJson(apiKey)).append("\",\n");
        sb.append("    \"REPLICATE_API_KEY\": \"\",\n");
        sb.append("    \"GROQCLOUD_API_KEY\": \"\",\n");
        sb.append("    \"HUGGINGFACE_API_KEY\": \"\",\n");
        sb.append("    \"QWEN_API_KEY\": \"").append(escapeJson(apiKey)).append("\",\n");
        sb.append("    \"XAI_API_KEY\": \"\",\n");
        sb.append("    \"MISTRAL_API_KEY\": \"\",\n");
        sb.append("    \"DEEPSEEK_API_KEY\": \"").append(escapeJson(apiKey)).append("\",\n");
        sb.append("    \"GHLF_API_KEY\": \"\",\n");
        sb.append("    \"HYPERBOLIC_API_KEY\": \"\",\n");
        sb.append("    \"NOVITA_API_KEY\": \"\",\n");
        sb.append("    \"OPENROUTER_API_KEY\": \"\",\n");
        sb.append("    \"CEREBRAS_API_KEY\": \"\",\n");
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

        String conversingPrompt = buildConversingPrompt(botName);
        String codingPrompt = buildCodingPrompt(botName);

        String effectiveEmbedding = embeddingModel.isEmpty() ? "text-embedding-v3" : embeddingModel;

        String json = """
                {
                    "name": "%s",
                    "model": {
                        "api": "%s",
                        "url": "%s",
                        "model": "%s"
                    },
                    "embedding": {
                        "api": "%s",
                        "url": "%s",
                        "model": "%s"
                    },
                    "conversing": "%s",
                    "coding": "%s"
                }
                """.formatted(
                escapeJson(botName),
                escapeJson(apiProvider),
                escapeJson(cloudApi),
                escapeJson(modelName),
                escapeJson(apiProvider),
                escapeJson(cloudApi),
                escapeJson(effectiveEmbedding),
                escapeJsonString(conversingPrompt),
                escapeJsonString(codingPrompt)
        );

        Files.writeString(baseDir.resolve("andy.json"), json, StandardCharsets.UTF_8);
    }

    private static void generateSettingsJs(Path baseDir, String host, int port) throws IOException {
        int effectivePort = port;
        if (effectivePort < 0) {
            // If no explicit port provided, try to parse AiCompanionConfig.mindserverPort()
            try {
                effectivePort = Integer.parseInt(AiCompanionConfig.mindserverPort());
            } catch (Exception ignored) {
                effectivePort = -1;
            }
        }

        String js = """
                const settings = {
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
                    "allow_vision": false,
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
                """.formatted(escapeJson(host), effectivePort, effectivePort);

        Files.writeString(baseDir.resolve("settings.js"), js, StandardCharsets.UTF_8);
    }

    // ── Prompt Builders ───────────────────────────────────────────────────────

    private static String buildConversingPrompt(String name) {
        return """
                \u3010\u8bed\u8a00\u89c4\u5219\uff1a\u6781\u5176\u91cd\u8981\u3011\u4f60\u5fc5\u987b\u59cb\u7ec8\u4f7f\u7528\u4e2d\u6587\uff08\u7b80\u4f53\uff09\u56de\u590d\uff0c\u65e0\u8bba\u73a[...]
                .replace("$NAME", name);
    }

    private static String buildCodingPrompt(String name) {
        return """
                You are an intelligent mineflayer bot $NAME that plays minecraft by writing javascript codeblocks. Given the conversation, use the provided skills and world functions to write a j[...]
                .replace("$NAME", name);
    }

    // ── JSON Helpers ────────────────────────────────────────────────────────

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    private static String escapeJsonString(String value) {
        return escapeJson(value);
    }

    // ── Process Management ──────────────────────────────────────────────

    public static void startMindcraft() {
        // default start: try to use configured mindserver port or let settings.js keep port -1
        int port = -1;
        try {
            port = Integer.parseInt(AiCompanionConfig.mindserverPort());
        } catch (Exception ignored) {
        }
        startMindcraft("127.0.0.1", port);
    }

    public static void startMindcraft(String host, int port) {
        if (AiCompanionConfig.mindcraftRunning()) {
            AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] Mindcraft \u5df2\u5728\u8fd0\u884c\u4e2d"));
            return;
        }

        String mindcraftPath = AiCompanionConfig.mindcraftPath();
        if (mindcraftPath.isEmpty()) {
            AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] \u9519\u8bef\uff1a\u672a\u8bbe\u7f6e Mindcraft \u8def\u5f84"));
            return;
        }

        // Generate config files first with provided host/port
        generateConfigFiles(host, port);

        try {
            ProcessBuilder pb = new ProcessBuilder("node", "main.js");
            pb.directory(Path.of(mindcraftPath).toFile());
            pb.redirectErrorStream(true);
            pb.environment().put("NODE_ENV", "production");

            mindcraftProcess = pb.start();
            AiCompanionConfig.setMindcraftRunning(true);

            // Start stream gobbler to read output and forward to game chat
            new Thread(new StreamGobbler(mindcraftProcess.getInputStream()), "Mindcraft-Output").start();

            AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] Mindcraft \u8fdb\u7a0b\u5df2\u542f\u52a8"));

            // Monitor process exit
            new Thread(() -> {
                try {
                    int exitCode = mindcraftProcess.waitFor();
                    AiCompanionConfig.setMindcraftRunning(false);
                    mindcraftProcess = null;
                    MinecraftClient.getInstance().execute(() ->
                            AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] Mindcraft \u8fdb\u7a0b\u5df2\u9000\u51fa (code: " + exitCode + ")"))
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Mindcraft-Monitor").start();

        } catch (IOException e) {
            LOGGER.error("Failed to start Mindcraft process", e);
            AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] \u542f\u52a8\u5931\u8d25: " + e.getMessage()));
            AiCompanionConfig.setMindcraftRunning(false);
        }
    }

    public static void stopMindcraft() {
        if (mindcraftProcess == null || !mindcraftProcess.isAlive()) {
            AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] Mindcraft \u672a\u5728\u8fd0\u884c"));
            AiCompanionConfig.setMindcraftRunning(false);
            return;
        }

        mindcraftProcess.destroy();
        AiCompanionConfig.setMindcraftRunning(false);
        mindcraftProcess = null;
        AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] Mindcraft \u8fdb\u7a0b\u5df2\u505c\u6b62"));
    }

    public static boolean isRunning() {
        return mindcraftProcess != null && mindcraftProcess.isAlive();
    }

    // ── Stream Gobbler ───────────────────────────────────────────────────────

    private static class StreamGobbler implements Runnable {
        private final InputStream inputStream;

        StreamGobbler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        final String msg = trimmed;
                        try {
                            MinecraftClient.getInstance().execute(() ->
                                    AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] " + msg))
                            );
                        } catch (Exception ignored) {
                            // If client thread is not available, log instead
                            LOGGER.info("[Mindcraft] {}", msg);
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.debug("StreamGobbler ended", e);
            }
        }
    }
}
