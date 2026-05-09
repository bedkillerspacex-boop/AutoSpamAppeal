package com.example.asa;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Blocks;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ASAClient implements ClientModInitializer {
    public static ASAState currentState = ASAState.IDLE;
    private long lastActionTime = 0;
    private int tickDelay = 0;

    private static KeyBinding configKey;

    @Override
    public void onInitializeClient() {
        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.autospamappeal.config",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "category.autospamappeal"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (configKey.wasPressed()) {
                client.setScreen(new ASAScreen());
            }
            this.onTick(client);
        });

        // Step 7: 检测聊天栏绿色提示并断开连接
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!ASAConfig.enabled) return;
            String content = message.getString();
            if (content.contains("反馈成功") || content.contains("Success")) {
                if (currentState == ASAState.FINISHING) {
                    MinecraftClient.getInstance().execute(() -> {
                        if (MinecraftClient.getInstance().world != null) {
                            MinecraftClient.getInstance().world.disconnect();
                            currentState = ASAState.IDLE;
                        }
                    });
                }
            }
        });
    }

    private void onTick(MinecraftClient client) {
        if (!ASAConfig.enabled || client.player == null || client.world == null) return;

        if (tickDelay > 0) {
            tickDelay--;
            return;
        }

        switch (currentState) {
            case IDLE -> {
                // 如果在某些特定情况下需要自动开始，可以在这里触发
                // 比如刚进服务器时
                if (client.player.getAbilities().invulnerable) { // 简单判断是否在保护期/初始点
                     currentState = ASAState.CHECKING_BLOCK;
                }
            }

            case CHECKING_BLOCK -> {
                // Step 2: 检测脚底方块 (图1中的方块，这里暂定为暗橡木板，请根据实际修改)
                if (client.world.getBlockState(client.player.getBlockPos().down()).isOf(Blocks.DARK_OAK_PLANKS)) {
                    client.player.sendMessage(Text.literal("§7[ASA] §a开始申诉..."), false);
                    currentState = ASAState.TELEPORTING;
                    tickDelay = 10;
                }
            }

            case TELEPORTING -> {
                // Step 3: TP 14 -59 14 (针对原版限制)
                // 发送位置包
                client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(14.5, -59.0, 14.5, true, true));
                client.player.setPosition(14.5, -59.0, 14.5);
                currentState = ASAState.ATTACKING;
                tickDelay = 5;
            }

            case ATTACKING -> {
                // Step 3: 攻击最近的假人 (Bot)
                Entity target = null;
                double minDesk = 5.0;
                Vec3d playerPos = client.player.getPos();

                for (Entity entity : client.world.getEntities()) {
                    if (entity != client.player) {
                        double dist = entity.getPos().distanceTo(playerPos);
                        if (dist < minDesk) {
                            target = entity;
                            minDesk = dist;
                        }
                    }
                }

                if (target != null) {
                    client.interactionManager.attackEntity(client.player, target);
                    currentState = ASAState.GUI_MAIN;
                    tickDelay = 20; // 等待 GUI 打开
                }
            }

            case GUI_MAIN -> {
                // Step 4: 处理 GUI (红石块)
                if (client.currentScreen instanceof GenericContainerScreen screen) {
                    if (screen.getTitle().getString().contains("反馈系统")) {
                        // 寻找红石块
                        for (int i = 0; i < screen.getScreenHandler().slots.size(); i++) {
                            var stack = screen.getScreenHandler().getSlot(i).getStack();
                            if (stack.isOf(net.minecraft.item.Items.REDSTONE_BLOCK)) {
                                clickSlot(client, screen, i);
                                currentState = ASAState.GUI_SUB;
                                tickDelay = 20;
                                return;
                            }
                        }
                    }
                }
            }

            case GUI_SUB -> {
                // Step 5: 处理子 GUI (骷髅头)
                if (client.currentScreen instanceof GenericContainerScreen screen) {
                    if (screen.getTitle().getString().contains("选择子类型")) {
                        for (int i = 0; i < screen.getScreenHandler().slots.size(); i++) {
                            var stack = screen.getScreenHandler().getSlot(i).getStack();
                            if (stack.isOf(net.minecraft.item.Items.PLAYER_HEAD)) {
                                if (stack.getName().getString().contains("自述申诉")) {
                                    clickSlot(client, screen, i);
                                    currentState = ASAState.WAITING_TITLE;
                                    tickDelay = 5;
                                    return;
                                }
                            }
                        }
                    }
                }
            }
            
            case WAITING_TITLE -> {
                // 逻辑在 Mixin 中处理，或者检测 GUI 关闭
                if (client.currentScreen == null) {
                    // GUI 已关闭，准备发送消息
                    // 状态转换由 Mixin 触发或在此处计时
                }
            }
        }
    }

    private void clickSlot(MinecraftClient client, GenericContainerScreen screen, int slotId) {
        client.interactionManager.clickSlot(screen.getScreenHandler().syncId, slotId, 0, SlotActionType.PICKUP, client.player);
    }
}
