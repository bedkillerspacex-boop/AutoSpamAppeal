package com.example.asa;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ASAConfig {
    public static boolean enabled = true;
    public static boolean showDebug = true;
    public static String appealReason = "系统误报，我正在正常游戏，请求解封。";
    public static String lastBanMessage = "";

    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("autospamappeal.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data != null) {
                enabled = data.enabled;
                showDebug = data.showDebug;
                appealReason = data.appealReason;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            ConfigData data = new ConfigData();
            data.enabled = enabled;
            data.showDebug = showDebug;
            data.appealReason = appealReason;
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ConfigData {
        boolean enabled;
        boolean showDebug;
        String appealReason;
    }
}
