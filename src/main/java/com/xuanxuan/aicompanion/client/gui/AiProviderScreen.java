package com.xuanxuan.aicompanion.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class AiProviderScreen extends Screen {
    private final Screen parent;

    public AiProviderScreen(Screen parent) {
        super(Text.translatable("screen.ai_companion.provider"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int startY = height / 2 - 38;

        addDrawableChild(ButtonWidget.builder(Text.translatable("button.ai_companion.cloud"), button ->
                client.setScreen(new CloudConfigScreen(this))
        ).dimensions(centerX - 105, startY, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("button.ai_companion.local"), button ->
                client.setScreen(new LocalProviderScreen(this))
        ).dimensions(centerX + 5, startY, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Mindcraft Bot"), button ->
                client.setScreen(new MindcraftConfigScreen(this))
        ).dimensions(centerX - 105, startY + 26, 210, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("返回"), button ->
                client.setScreen(parent)
        ).dimensions(centerX - 50, startY + 62, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 72, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("选择 AI 服务来源"), width / 2, height / 2 - 56, 0xAAAAAA);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
