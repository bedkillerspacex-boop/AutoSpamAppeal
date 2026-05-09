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
        handleText(title);
    }

    @Inject(method = "setSubtitle", at = @At("HEAD"))
    private void onSetSubtitle(Text subtitle, CallbackInfo ci) {
        handleText(subtitle);
    }

    private void handleText(Text text) {
        if (!ASAConfig.enabled || text == null) return;

        String content = text.getString();
        // 匹配图片中的文字：醒醒！ 请在聊天框发送你要反馈的内容
        if (content.contains("醒醒") || (content.contains("发送") && content.contains("反馈") && content.contains("内容"))) {
            if (ASAClient.currentState == ASAState.WAITING_TITLE) {
                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.networkHandler.sendChatMessage(ASAConfig.appealReason);
                        ASAClient.currentState = ASAState.FINISHING;
                        client.player.sendMessage(Text.literal("§7[ASA] §a检测到 Title，已发送内容: §f" + ASAConfig.appealReason), false);
                    }
                });
            }
        }
    }
}
