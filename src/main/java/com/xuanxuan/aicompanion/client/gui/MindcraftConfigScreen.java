package com.xuanxuan.aicompanion.client.gui;

import com.xuanxuan.aicompanion.client.AiCompanionClient;
import com.xuanxuan.aicompanion.client.config.AiCompanionConfig;
import com.xuanxuan.aicompanion.client.mindcraft.MindcraftProcessManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class MindcraftConfigScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget botNameInput;
    private TextFieldWidget apiProtocolInput;
    private TextFieldWidget apiUrlInput;
    private TextFieldWidget apiKeyInput;
    private TextFieldWidget modelNameInput;
    private TextFieldWidget mindcraftPathInput;

    public MindcraftConfigScreen(Screen parent) {
        super(Text.translatable("screen.ai_companion.mindcraft_config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int startY = height / 2 - 80;
        int labelX = centerX - 120;
        int fieldWidth = 240;

        botNameInput = new TextFieldWidget(textRenderer, labelX, startY + 14, fieldWidth, 18, Text.literal(""));
        botNameInput.setText(AiCompanionConfig.botName());
        botNameInput.setMaxLength(16);
        addDrawableChild(botNameInput);

        apiProtocolInput = new TextFieldWidget(textRenderer, labelX, startY + 40, fieldWidth, 18, Text.literal(""));
        apiProtocolInput.setText(AiCompanionConfig.apiProvider());
        apiProtocolInput.setMaxLength(64);
        addDrawableChild(apiProtocolInput);

        apiUrlInput = new TextFieldWidget(textRenderer, labelX, startY + 66, fieldWidth, 18, Text.literal(""));
        apiUrlInput.setText(AiCompanionConfig.cloudApi());
        apiUrlInput.setMaxLength(512);
        addDrawableChild(apiUrlInput);

        apiKeyInput = new TextFieldWidget(textRenderer, labelX, startY + 92, fieldWidth, 18, Text.literal(""));
        apiKeyInput.setText(AiCompanionConfig.apiKey());
        apiKeyInput.setMaxLength(256);
        addDrawableChild(apiKeyInput);

        modelNameInput = new TextFieldWidget(textRenderer, labelX, startY + 118, fieldWidth, 18, Text.literal(""));
        modelNameInput.setText(AiCompanionConfig.modelName());
        modelNameInput.setMaxLength(128);
        addDrawableChild(modelNameInput);

        mindcraftPathInput = new TextFieldWidget(textRenderer, labelX, startY + 144, fieldWidth, 18, Text.literal(""));
        mindcraftPathInput.setText(AiCompanionConfig.mindcraftPath());
        mindcraftPathInput.setMaxLength(512);
        addDrawableChild(mindcraftPathInput);

        int btnY = startY + 170;
        addDrawableChild(ButtonWidget.builder(Text.literal("\u4fdd\u5b58\u914d\u7f6e"), button -> saveConfig(false))
                .dimensions(centerX - 157, btnY, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("\u4fdd\u5b58\u5e76\u542f\u52a8 Bot"), button -> saveConfig(true))
                .dimensions(centerX - 50, btnY, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("\u505c\u6b62 Bot"), button -> stopBot())
                .dimensions(centerX + 57, btnY, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("\u8fd4\u56de"), button -> client.setScreen(parent))
                .dimensions(centerX - 50, btnY + 26, 100, 20).build());
    }

    private void saveConfig(boolean startBot) {
        String botName = botNameInput.getText().trim();
        if (botName.length() < 3 || botName.length() > 16 || !botName.matches("[a-zA-Z0-9_]+")) {
            AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] Bot\u73a9\u5bb6\u540d\u9700\u4e3a3-16\u4f4d\u5b57\u6bcd\u6570\u5b57\u4e0b\u5212\u7ebf"));
            return;
        }

        AiCompanionConfig.setBotName(botName);
        AiCompanionConfig.setApiProvider(apiProtocolInput.getText().trim());
        AiCompanionConfig.setCloudApi(apiUrlInput.getText().trim());
        AiCompanionConfig.setApiKey(apiKeyInput.getText().trim());
        AiCompanionConfig.setModelName(modelNameInput.getText().trim());
        AiCompanionConfig.setMindcraftPath(mindcraftPathInput.getText().trim());
        AiCompanionConfig.save();

        if (startBot) {
            MindcraftProcessManager.generateConfigFiles();
            MindcraftProcessManager.startMindcraft();
        }

        AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] \u914d\u7f6e\u5df2\u4fdd\u5b58"));
    }

    private void stopBot() {
        MindcraftProcessManager.stopMindcraft();
        AiCompanionClient.addChatMessage(Text.literal("[Mindcraft] \u6b63\u5728\u505c\u6b62 Bot..."));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = width / 2;
        int startY = height / 2 - 80;
        int labelX = centerX - 120;
        int grayColor = 0xAAAAAA;

        context.drawCenteredTextWithShadow(textRenderer, title, centerX, startY - 8, 0xFFFFFF);

        context.drawTextWithShadow(textRenderer, Text.literal("Bot \u73a9\u5bb6\u540d"), labelX, startY + 2, grayColor);
        context.drawTextWithShadow(textRenderer, Text.literal("API \u534f\u8bae"), labelX, startY + 28, grayColor);
        context.drawTextWithShadow(textRenderer, Text.literal("API \u5730\u5740"), labelX, startY + 54, grayColor);
        context.drawTextWithShadow(textRenderer, Text.literal("API Key"), labelX, startY + 80, grayColor);
        context.drawTextWithShadow(textRenderer, Text.literal("\u6a21\u578b\u540d"), labelX, startY + 106, grayColor);
        context.drawTextWithShadow(textRenderer, Text.literal("Mindcraft \u8def\u5f84"), labelX, startY + 132, grayColor);

        int statusY = startY + 226;
        boolean running = MindcraftProcessManager.isRunning();
        Text statusText = running
                ? Text.literal("Bot \u72b6\u6001: ").append(Text.literal("\u8fd0\u884c\u4e2d").withColor(0x55FF55))
                : Text.literal("Bot \u72b6\u6001: ").append(Text.literal("\u672a\u542f\u52a8").withColor(0xFF5555));
        context.drawCenteredTextWithShadow(textRenderer, statusText, centerX, statusY, 0xFFFFFF);

        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("API \u534f\u8bae\u586b\u5199 openai\uff08\u517c\u5bb9\u963f\u91cc\u4e91\u767e\u70bc\u7b49 OpenAI \u683c\u5f0f\u63a5\u53e3\uff09"),
                centerX, statusY + 16, 0x888888);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
