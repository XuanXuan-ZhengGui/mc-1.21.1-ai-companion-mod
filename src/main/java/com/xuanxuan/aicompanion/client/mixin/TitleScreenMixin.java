package com.xuanxuan.aicompanion.client.mixin;

import com.xuanxuan.aicompanion.client.gui.AiBotConnectScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void aiCompanion$addAiBotButton(CallbackInfo ci) {
        int centerX = width / 2;
        int y = height - 40;
        addDrawableChild(ButtonWidget.builder(Text.literal("AI Bot 客户端"), button ->
                client.setScreen(new AiBotConnectScreen(this))
        ).dimensions(centerX - 100, y, 200, 20).build());
    }
}
