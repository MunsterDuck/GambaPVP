package com.munsterduck.gambapvp.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class KitManager {

    private static File getKitsDirectory(MinecraftServer server) {
        File worldDir = server.getSavePath(WorldSavePath.ROOT).toFile();
        File kitsDir = new File(worldDir, "gkits");

        if (!kitsDir.exists()) {
            kitsDir.mkdirs();
        }

        return kitsDir;
    }

    private static File getKitFile(MinecraftServer server, String kitName) {
        return new File(getKitsDirectory(server), kitName + ".nbt");
    }

    public static void saveKit(MinecraftServer server, String kitName, NbtCompound kitData) {
        try {
            File kitFile = getKitFile(server, kitName);
            NbtIo.writeCompressed(kitData, kitFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static NbtCompound loadKit(MinecraftServer server, String kitName) {
        try {
            File kitFile = getKitFile(server, kitName);

            if (!kitFile.exists()) {
                return null;
            }

            return NbtIo.readCompressed(kitFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<String> getKitNames(MinecraftServer server) {
        List<String> kitNames = new ArrayList<>();
        File kitsDir = getKitsDirectory(server);

        File[] files = kitsDir.listFiles((dir, name) -> name.endsWith(".nbt"));

        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                kitNames.add(name.substring(0, name.length() - 4)); // Remove .nbt extension
            }
        }

        return kitNames;
    }

    public static boolean deleteKit(MinecraftServer server, String kitName) {
        File kitFile = getKitFile(server, kitName);
        return kitFile.exists() && kitFile.delete();
    }
}