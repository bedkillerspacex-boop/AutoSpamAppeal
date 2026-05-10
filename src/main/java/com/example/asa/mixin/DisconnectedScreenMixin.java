package com.example.asa.mixin;

import com.example.asa.ASAConfig;
import com.example.asa.ASAUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
    @Unique
    private Text asa$capturedReason;

    protected DisconnectedScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/text/Text;Lnet/minecraft/text/Text;)V", at = @At("TAIL"))
    private void onInit3(Screen parent, Text title, Text reason, CallbackInfo ci) {
        this.asa$capturedReason = reason;
    }

    @Inject(method = "<init>(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/text/Text;Lnet/minecraft/text/Text;Lnet/minecraft/text/Text;)V", at = @At("TAIL"))
    private void onInit4(Screen parent, Text title, Text reason, Text buttonLabel, CallbackInfo ci) {
        this.asa$capturedReason = reason;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInitScreen(CallbackInfo ci) {
        if (!ASAConfig.enabled || this.asa$capturedReason == null) return;

        String reasonStr = this.asa$capturedReason.getString();
        // Step 1: 检测 Banned 消息
        if (reasonStr.contains("封禁") || reasonStr.contains("Banned") || reasonStr.contains("检测到") || reasonStr.contains("Spam")) {
            ASAConfig.lastBanMessage = reasonStr;
            // 延迟重连
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // 1秒后重连
                    MinecraftClient.getInstance().execute(() -> {
                        ASAUtils.reconnect(MinecraftClient.getInstance());
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
