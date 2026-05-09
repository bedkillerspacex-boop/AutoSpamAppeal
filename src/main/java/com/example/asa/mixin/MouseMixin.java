package com.example.asa.mixin;

import com.example.asa.ASAClient;
import com.example.asa.ASAState;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    // 拦截鼠标点击
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (ASAClient.currentState != ASAState.IDLE) {
            ci.cancel();
        }
    }

    // 拦截鼠标移动（视角转动）
    @Inject(method = "onCursorPos", at = @At("HEAD"), cancellable = true)
    private void onCursorPos(long window, double x, double y, CallbackInfo ci) {
        if (ASAClient.currentState != ASAState.IDLE) {
            ci.cancel();
        }
    }
    
    // 拦截滚轮
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (ASAClient.currentState != ASAState.IDLE) {
            ci.cancel();
        }
    }
}
