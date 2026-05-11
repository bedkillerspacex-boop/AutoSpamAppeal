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

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
    @Unique
    private Text asa$capturedReason;
    @Unique
    private final AtomicBoolean asa$rejoinScheduled = new AtomicBoolean(false);

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
        boolean isBan = reasonStr.contains("封禁") || reasonStr.contains("Banned") || reasonStr.contains("检测到") || reasonStr.contains("Spam");
        boolean shouldReconnect = ASAUtils.shouldAutoReconnect(reasonStr);

        if (shouldReconnect && asa$rejoinScheduled.compareAndSet(false, true)) {
            ASAConfig.lastBanMessage = isBan ? reasonStr : ASAConfig.lastBanMessage;
            ASAUtils.scheduleReconnect(MinecraftClient.getInstance(), 1200);
        }
    }
}
