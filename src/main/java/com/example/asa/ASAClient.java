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
import net.minecraft.registry.tag.BlockTags;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ASAClient implements ClientModInitializer {
    public static ASAState currentState = ASAState.IDLE;
    private int tickDelay = 0;

    private static KeyBinding configKey;
    private static KeyBinding forceStartKey;

    @Override
    public void onInitializeClient() {
        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.autospamappeal.config",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "category.autospamappeal"
        ));

        forceStartKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.autospamappeal.force",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "category.autospamappeal"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            
            while (configKey.wasPressed()) {
                client.setScreen(new ASAScreen());
            }
            while (forceStartKey.wasPressed()) {
                currentState = ASAState.TELEPORTING;
                client.player.sendMessage(Text.literal("§7[ASA] §e强制开始申诉流程..."), false);
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

        // 调试信息：在 Action Bar 显示当前状态
        if (currentState != ASAState.IDLE) {
            client.player.sendMessage(Text.literal("§b[ASA 状态] §f" + currentState.name()), true);
        }

        if (tickDelay > 0) {
            tickDelay--;
            return;
        }

        switch (currentState) {
            case IDLE -> {
                // 启用后立即进入检测阶段
                currentState = ASAState.CHECKING_BLOCK;
            }

            case CHECKING_BLOCK -> {
                // Step 2: 检测脚底方块 (匹配图 1 中的多样化木板和闪长岩)
                var blockState = client.world.getBlockState(client.player.getBlockPos().down());
                boolean matches = blockState.isIn(BlockTags.PLANKS) || 
                                 blockState.isOf(Blocks.DIORITE) || 
                                 blockState.isOf(Blocks.POLISHED_DIORITE) ||
                                 blockState.isOf(Blocks.ANDESITE) ||
                                 blockState.isOf(Blocks.GRANITE);

                if (matches) {
                    client.player.sendMessage(Text.literal("§7[ASA] §a检测到申诉环境，开始申诉..."), false);
                    currentState = ASAState.TELEPORTING;
                    tickDelay = 10;
                }
            }

            case TELEPORTING -> {
                // Step 3: TP 14 -59 14
                client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(14.5, -59.0, 14.5, true, true));
                client.player.setPosition(14.5, -59.0, 14.5);
                currentState = ASAState.ATTACKING;
                tickDelay = 10;
            }

            case ATTACKING -> {
                // Step 3: 攻击最近的假人
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
                    tickDelay = 30; // 稍长的等待，确保 GUI 开启
                }
            }

            case GUI_MAIN -> {
                // Step 4: 处理主 GUI
                if (client.currentScreen instanceof GenericContainerScreen screen) {
                    if (screen.getTitle().getString().contains("反馈系统")) {
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
                // Step 5: 处理子 GUI
                if (client.currentScreen instanceof GenericContainerScreen screen) {
                    if (screen.getTitle().getString().contains("选择子类型")) {
                        for (int i = 0; i < screen.getScreenHandler().slots.size(); i++) {
                            var stack = screen.getScreenHandler().getSlot(i).getStack();
                            if (stack.isOf(net.minecraft.item.Items.PLAYER_HEAD) || stack.isOf(net.minecraft.item.Items.SKELETON_SKULL)) {
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
                // Mixin 处理发送
                if (client.currentScreen == null && tickDelay == 0) {
                     // 如果长时间没识别到 Title，重置状态
                     // 可以在这里加超时处理
                }
            }
        }
    }

    private void clickSlot(MinecraftClient client, GenericContainerScreen screen, int slotId) {
        client.interactionManager.clickSlot(screen.getScreenHandler().syncId, slotId, 0, SlotActionType.PICKUP, client.player);
    }
}
