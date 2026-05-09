package com.example.asa.mixin;

import com.example.asa.ASAClient;
import com.example.asa.ASAState;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void onKey(long window, int key, int scancode, int action, int mods, CallbackInfo ci) {
        // 如果自动化正在运行
        if (ASAClient.currentState != ASAState.IDLE) {
            // 允许 ESC 键退出自动化
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                ASAClient.currentState = ASAState.IDLE;
                MinecraftClient.getInstance().player.sendMessage(net.minecraft.text.Text.literal("§7[ASA] §c用户干预，已手动停止任务。"), false);
                return;
            }
            
            // 屏蔽其他所有按键
            ci.cancel();
        }
    }
}
