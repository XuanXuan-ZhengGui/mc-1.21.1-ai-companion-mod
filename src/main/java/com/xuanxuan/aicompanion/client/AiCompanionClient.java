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
            }
        } catch (Exception exception) {
            addChatMessage(Text.literal("[系统] 开启局域网失败：" + exception.getMessage()));
        }
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
}
