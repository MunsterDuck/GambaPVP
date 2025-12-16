package com.munsterduck.gambapvp.util;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class KitManager {

    public static final SuggestionProvider<ServerCommandSource> KIT_SUGGESTIONS = (context, builder) -> {
        String remaining = builder.getRemaining().toLowerCase();

        KitManager.getKitNames(context.getSource().getServer()).stream()
                .filter(kitName -> kitName.toLowerCase().startsWith(remaining))
                .forEach(builder::suggest);

        return builder.buildFuture();
    };

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

    public static void saveKit(MinecraftServer server, KitData kitData) {
        try {
            File kitFile = getKitFile(server, kitData.getName());
            NbtCompound fullKitData = kitData.toNbt();
            NbtIo.writeCompressed(fullKitData, kitFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static KitData loadKitWithIcon(MinecraftServer server, String kitName) {
        try {
            File kitFile = getKitFile(server, kitName);

            if (!kitFile.exists()) {
                return null;
            }

            NbtCompound fullKitData = NbtIo.readCompressed(kitFile);
            return KitData.fromNbt(fullKitData);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static NbtCompound loadKit(MinecraftServer server, String kitName) {
        KitData kitData = loadKitWithIcon(server, kitName);
        return kitData != null ? kitData.getInventoryData() : null;
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

    public static List<KitData> getAllKitsWithIcons(MinecraftServer server) {
        List<KitData> kits = new ArrayList<>();
        File kitsDir = getKitsDirectory(server);

        File[] files = kitsDir.listFiles((dir, name) -> name.endsWith(".nbt"));

        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                String kitName = name.substring(0, name.length() - 4); // Remove .nbt extension
                KitData kitData = loadKitWithIcon(server, kitName);
                if (kitData != null) {
                    kits.add(kitData);
                }
            }
        }

        return kits;
    }

    public static boolean deleteKit(MinecraftServer server, String kitName) {
        File kitFile = getKitFile(server, kitName);
        return kitFile.exists() && kitFile.delete();
    }
}