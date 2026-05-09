package com.example.asa.mixin;

import com.example.asa.ASAClient;
import com.example.asa.ASAConfig;
import com.example.asa.ASAState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "setTitle", at = @At("HEAD"))
    private void onSetTitle(Text title, CallbackInfo ci) {
        if (!ASAConfig.enabled) return;

        String titleStr = title.getString();
        // Step 6: 识别 Title "请在聊天框发送..."
        if (titleStr.contains("发送你反馈的内容")) {
            if (ASAClient.currentState == ASAState.WAITING_TITLE) {
                // 延迟 1 tick 发送
                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.networkHandler.sendChatMessage(ASAConfig.appealReason);
                        ASAClient.currentState = ASAState.FINISHING;
                    }
                });
            }
        }
    }
}
