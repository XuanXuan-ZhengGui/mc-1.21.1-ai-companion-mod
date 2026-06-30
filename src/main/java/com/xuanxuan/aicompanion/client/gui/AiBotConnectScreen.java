package com.xuanxuan.aicompanion.client.gui;

import com.xuanxuan.aicompanion.client.AiCompanionClient;
import com.xuanxuan.aicompanion.client.config.AiCompanionConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.text.Text;

public final class AiBotConnectScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget ipInput;

    public AiBotConnectScreen(Screen parent) {
        super(Text.literal("AI Bot 客户端"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int startY = height / 2 - 40;

        ipInput = new TextFieldWidget(textRenderer, centerX - 120, startY + 20, 240, 20, Text.literal("IP:端口"));
        ipInput.setText(AiCompanionConfig.targetServerIp());
        ipInput.setMaxLength(256);
        addDrawableChild(ipInput);

        addDrawableChild(ButtonWidget.builder(Text.literal("连接并启动 AI"), button -> connect())
                .dimensions(centerX - 120, startY + 56, 240, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("返回"), button -> client.setScreen(parent))
                .dimensions(centerX - 120, startY + 86, 240, 20).build());
    }

    private void connect() {
        String address = ipInput.getText().trim();
        if (address.isBlank()) {
            AiCompanionClient.addChatMessage(Text.literal("[AI Bot] 请输入 IP:端口"));
            return;
        }

        AiCompanionConfig.setAiBotMode(true);
        AiCompanionConfig.setTargetServerIp(address);
        AiCompanionConfig.save();

        try {
            ServerAddress serverAddress = ServerAddress.parse(address);
            ServerInfo serverInfo = new ServerInfo("AI Target", address, ServerInfo.ServerType.OTHER);
            ConnectScreen.connect(this, client, serverAddress, serverInfo, false);
        } catch (Exception exception) {
            AiCompanionClient.addChatMessage(Text.literal("[AI Bot] 连接失败：" + exception.getMessage()));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = width / 2;
        int startY = height / 2 - 40;
        context.drawCenteredTextWithShadow(textRenderer, title, centerX, startY - 16, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("输入目标局域网 IP:端口"), centerX - 120, startY + 6, 0xAAAAAA);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
