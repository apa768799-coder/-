package net.smartminer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(MinecraftClient.getInstance().runDirectory, "config/smartminer.json");

    public static void saveConfig(Modules modules) {
        try {
            if (!CONFIG_FILE.getParentFile().exists()) {
                CONFIG_FILE.getParentFile().mkdirs();
            }
            FileWriter writer = new FileWriter(CONFIG_FILE);
            GSON.toJson(modules, writer);
            writer.close();
            SmartClient.addLog("§a[Config] Настройки успешно сохранены!");
        } catch (Exception e) {
            SmartClient.addLog("§c[Config] Ошибка сохранения: " + e.getMessage());
        }
    }

    public static void loadConfig(Modules modules) {
        try {
            if (!CONFIG_FILE.exists()) {
                SmartClient.addLog("§e[Config] Файл конфига не найден, используются стандартные настройки.");
                return;
            }
            FileReader reader = new FileReader(CONFIG_FILE);
            Modules loaded = GSON.fromJson(reader, Modules.class);
            reader.close();
            
            // Копируем загруженные значения в текущий объект
            if (loaded != null) {
                modules.copyFrom(loaded);
                SmartClient.addLog("§a[Config] Настройки успешно загружены!");
            }
        } catch (Exception e) {
            SmartClient.addLog("§c[Config] Ошибка загрузки: " + e.getMessage());
        }
    }
}
