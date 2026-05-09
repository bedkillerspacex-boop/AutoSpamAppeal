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
    public static int tickDelay = 0;

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
                    // 等待 2 秒后退出
                    tickDelay = 40; 
                    new Thread(() -> {
                        try {
                            Thread.sleep(2000); // 2秒
                            MinecraftClient.getInstance().execute(() -> {
                                if (MinecraftClient.getInstance().world != null && currentState == ASAState.FINISHING) {
                                    MinecraftClient.getInstance().world.disconnect();
                                    currentState = ASAState.IDLE;
                                }
                            });
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            }
        });
    }

    private void onTick(MinecraftClient client) {
        if (!ASAConfig.enabled || client.player == null || client.world == null) return;

        // 调试信息：在 Action Bar 显示当前状态
        if (ASAConfig.showDebug) {
            if (currentState != ASAState.IDLE && tickDelay == 0) {
                client.player.sendMessage(Text.literal("§b[ASA 状态] §f" + currentState.name()), true);
            } else if (tickDelay > 0 && currentState == ASAState.IDLE) {
                client.player.sendMessage(Text.literal("§c[ASA 冷却中] §f" + (tickDelay / 20 + 1) + "s"), true);
            }
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
                // 新增坐标限制：X: 12.5 +/- 5, Z: 10.5 +/- 5, Y: 必须是 -59
                double playerX = client.player.getX();
                double playerY = client.player.getY();
                double playerZ = client.player.getZ();

                boolean inPosition = Math.abs(playerX - 12.5) <= 5.0 && 
                                   Math.abs(playerZ - 10.5) <= 5.0 && 
                                   (int)Math.floor(playerY) == -59;

                if (!inPosition) return;

                // Step 2: 检测脚底方块 3x3 范围内是否有 5 个以上目标方块
                int matchCount = 0;
                var basePos = client.player.getBlockPos().down();
                
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        var pos = basePos.add(x, 0, z);
                        var state = client.world.getBlockState(pos);
                        if (state.isIn(BlockTags.PLANKS) || 
                            state.isOf(Blocks.DIORITE) || 
                            state.isOf(Blocks.POLISHED_DIORITE) ||
                            state.isOf(Blocks.ANDESITE) ||
                            state.isOf(Blocks.GRANITE)) {
                            matchCount++;
                        }
                    }
                }

                if (matchCount >= 5) {
                    client.player.sendMessage(Text.literal("§7[ASA] §a检测到周围有 " + matchCount + " 个目标方块，开始申诉..."), false);
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
