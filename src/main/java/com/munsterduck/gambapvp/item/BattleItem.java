package com.munsterduck.gambapvp.item;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public class BattleItem {
    private static final String BATTLE_ITEM_TAG = "BattleItem";
    private static final String BATTLE_ID_TAG = "BattleID";

    public static ItemStack markAsBattleItem(ItemStack stack, String battleId) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putBoolean(BATTLE_ITEM_TAG, true);
        nbt.putString(BATTLE_ID_TAG, battleId);
        return stack;
    }

    public static boolean isBattleItem(ItemStack stack) {
        return stack.hasNbt() && stack.getNbt().getBoolean(BATTLE_ITEM_TAG);
    }

    public static String getBattleId(ItemStack stack) {
        if (stack.hasNbt()) {
            return stack.getNbt().getString(BATTLE_ID_TAG);
        }
        return "";
    }

    public static void removeBattleTag(ItemStack stack) {
        if (stack.hasNbt()) {
            NbtCompound nbt = stack.getNbt();
            nbt.remove(BATTLE_ITEM_TAG);
            nbt.remove(BATTLE_ID_TAG);
        }
    }
}
