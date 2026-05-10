package com.example.asa;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

import net.minecraft.text.Text;

public class ASAUtils {
    public static ServerInfo lastServerInfo;

    public static void reconnect(MinecraftClient client) {
        ServerInfo info = client.getCurrentServerEntry() != null ? client.getCurrentServerEntry() : lastServerInfo;
        if (info != null) {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§7[ASA] §e正在尝试重连至: " + info.address), false);
            }
            ConnectScreen.connect(new TitleScreen(), client, ServerAddress.parse(info.address), info, false, null);
            ASAClient.currentState = ASAState.RECONNECTING;
        } else {
            System.out.println("[ASA] 无法重连：未找到服务器信息");
        }
    }
}
