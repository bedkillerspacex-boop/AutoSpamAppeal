package com.example.asa;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

public class ASAUtils {
    public static void reconnect(MinecraftClient client) {
        ServerInfo info = client.getCurrentServerEntry();
        if (info != null) {
            ConnectScreen.connect(new TitleScreen(), client, ServerAddress.parse(info.address), info, false, null);
            ASAClient.currentState = ASAState.RECONNECTING;
        }
    }
}
