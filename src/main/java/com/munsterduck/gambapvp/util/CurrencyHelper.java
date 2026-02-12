package com.munsterduck.gambapvp.util;

import com.munsterduck.gambapvp.GambaPVP;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Helper for interacting with NotchCurrency via reflection.
 * Uses the server-side API: net.fugginbeenus.notchcurrency.api.CurrencyApi
 */
public class CurrencyHelper {
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("notchcurrency");
    private static final String API_CLASS = "net.fugginbeenus.notchcurrency.api.CurrencyApi";

    public static boolean isAvailable() {
        return LOADED;
    }

    /**
     * Get a player's current balance.
     */
    public static int getBalance(ServerPlayerEntity player) {
        if (!LOADED) return 0;
        try {
            Class<?> api = Class.forName(API_CLASS);
            Object result = api.getMethod("getBalance", ServerPlayerEntity.class).invoke(null, player);
            return ((Number) result).intValue();
        } catch (Exception e) {
            GambaPVP.LOGGER.error("Failed to get currency balance", e);
            return 0;
        }
    }

    /**
     * Deposit (add) currency to a player's balance.
     */
    public static boolean deposit(ServerPlayerEntity player, int amount) {
        if (!LOADED || amount <= 0) return false;
        try {
            Class<?> api = Class.forName(API_CLASS);
            api.getMethod("deposit", ServerPlayerEntity.class, int.class).invoke(null, player, amount);
            return true;
        } catch (Exception e) {
            GambaPVP.LOGGER.error("Failed to deposit {} currency to {}", amount, player.getName().getString(), e);
            return false;
        }
    }

    /**
     * Withdraw (deduct) currency from a player's balance.
     * Returns true if successful, false if insufficient funds or error.
     */
    public static boolean withdraw(ServerPlayerEntity player, int amount) {
        if (!LOADED || amount <= 0) return false;
        try {
            Class<?> api = Class.forName(API_CLASS);
            Object result = api.getMethod("withdraw", ServerPlayerEntity.class, int.class).invoke(null, player, amount);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            GambaPVP.LOGGER.error("Failed to withdraw {} currency from {}", amount, player.getName().getString(), e);
            return false;
        }
    }
}
