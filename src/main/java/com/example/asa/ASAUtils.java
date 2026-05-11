package com.example.asa;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

import net.minecraft.text.Text;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ASAUtils {
    public static ServerInfo lastServerInfo;
    private static final ScheduledExecutorService RECONNECT_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "asa-rejoin");
        t.setDaemon(true);
        return t;
    });

    public static boolean shouldAutoReconnect(String reasonStr) {
        String lower = reasonStr == null ? "" : reasonStr.toLowerCase(Locale.ROOT);
        return lower.contains("封禁")
            || lower.contains("banned")
            || lower.contains("spam")
            || lower.contains("检测到")
            || lower.contains("disconnected")
            || lower.contains("连接已断开");
    }

    public static void scheduleReconnect(MinecraftClient client, long delayMs) {
        RECONNECT_EXECUTOR.schedule(() -> client.execute(() -> reconnect(client)), delayMs, TimeUnit.MILLISECONDS);
    }

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
