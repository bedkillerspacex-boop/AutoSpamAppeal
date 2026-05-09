package com.example.asa.mixin;

import com.example.asa.ASAClient;
import com.example.asa.ASAConfig;
import com.example.asa.ASAState;
import com.example.asa.ASAUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
    @Shadow @Final private Text reason;

    protected DisconnectedScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (!ASAConfig.enabled) return;

        String reasonStr = reason.getString();
        // Step 1: 检测 Banned 消息 (根据实际情况调整关键词)
        if (reasonStr.contains("封禁") || reasonStr.contains("Banned") || reasonStr.contains("检测到")) {
            ASAConfig.lastBanMessage = reasonStr;
            // 延迟重连，避免过于频繁
            new Thread(() -> {
                try {
                    Thread.sleep(5000); // 5秒后重连
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
