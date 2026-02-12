package com.munsterduck.gambapvp.battle;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

/**
 * Manages the pot for a duel using poker-style logic.
 * Each player can only win back what they contributed (matched amounts).
 */
public class WagerPot {
    private final Map<UUID, WagerData> playerWagers;
    private final Map<UUID, WagerData> playerMaxWinnings; // Maximum each player can win

    public WagerPot() {
        this.playerWagers = new HashMap<>();
        this.playerMaxWinnings = new HashMap<>();
    }

    /**
     * Add a player's wager to the pot
     */
    public void addWager(UUID playerUuid, WagerData wager) {
        playerWagers.put(playerUuid, wager.copy());
        calculateMaxWinnings();
    }

    /**
     * Calculate maximum winnings for each player using poker pot logic.
     * A player can only win matched amounts from each opponent.
     */
    private void calculateMaxWinnings() {
        playerMaxWinnings.clear();

        for (UUID playerUuid : playerWagers.keySet()) {
            WagerData playerWager = playerWagers.get(playerUuid);
            WagerData maxWinnings = new WagerData();

            // Player gets their own wager back
            for (ItemStack stack : playerWager.getItems()) {
                maxWinnings.addItem(stack.copy());
            }
            maxWinnings.setCurrency(playerWager.getCurrency());

            // Add matched amounts from each opponent
            for (UUID opponentUuid : playerWagers.keySet()) {
                if (opponentUuid.equals(playerUuid)) continue;

                WagerData opponentWager = playerWagers.get(opponentUuid);

                // Match items (simplified: match by item type up to player's wager amount)
                Map<String, Integer> playerItemCounts = getItemCounts(playerWager.getItems());
                Map<String, Integer> opponentItemCounts = getItemCounts(opponentWager.getItems());

                for (String itemKey : playerItemCounts.keySet()) {
                    if (opponentItemCounts.containsKey(itemKey)) {
                        int playerCount = playerItemCounts.get(itemKey);
                        int opponentCount = opponentItemCounts.get(itemKey);
                        int matchedCount = Math.min(playerCount, opponentCount);

                        // Add matched items to winnings
                        ItemStack matchedStack = findItemStackByKey(opponentWager.getItems(), itemKey);
                        if (matchedStack != null && matchedCount > 0) {
                            ItemStack winStack = matchedStack.copy();
                            winStack.setCount(matchedCount);
                            maxWinnings.addItem(winStack);
                        }
                    }
                }

                // Match currency
                int playerCurrency = playerWager.getCurrency();
                int opponentCurrency = opponentWager.getCurrency();
                int matchedCurrency = Math.min(playerCurrency, opponentCurrency);
                maxWinnings.setCurrency(maxWinnings.getCurrency() + matchedCurrency);
            }

            playerMaxWinnings.put(playerUuid, maxWinnings);
        }
    }

    /**
     * Get the maximum winnings for a player
     */
    public WagerData getMaxWinnings(UUID playerUuid) {
        WagerData winnings = playerMaxWinnings.get(playerUuid);
        return winnings != null ? winnings.copy() : new WagerData();
    }

    /**
     * Get a player's original wager
     */
    public WagerData getPlayerWager(UUID playerUuid) {
        WagerData wager = playerWagers.get(playerUuid);
        return wager != null ? wager.copy() : new WagerData();
    }

    /**
     * Distribute winnings to the winner and return remaining items to losers
     */
    public Map<UUID, WagerData> distributeWinnings(UUID winnerUuid) {
        Map<UUID, WagerData> distribution = new HashMap<>();

        // Winner gets their maximum winnings
        WagerData winnings = getMaxWinnings(winnerUuid);
        distribution.put(winnerUuid, winnings);

        // Calculate what's left over for losers
        WagerData winnerOriginalWager = playerWagers.get(winnerUuid);

        for (UUID loserUuid : playerWagers.keySet()) {
            if (loserUuid.equals(winnerUuid)) continue;

            WagerData loserWager = playerWagers.get(loserUuid);
            WagerData loserReturns = new WagerData();

            if (winnerOriginalWager == null) {
                // Winner had no wager - return everything to loser
                for (ItemStack stack : loserWager.getItems()) {
                    loserReturns.addItem(stack.copy());
                }
                loserReturns.setCurrency(loserWager.getCurrency());
            } else {
                // Return unmatched items to loser
                Map<String, Integer> winnerItemCounts = getItemCounts(winnerOriginalWager.getItems());
                Map<String, Integer> loserItemCounts = getItemCounts(loserWager.getItems());

                for (String itemKey : loserItemCounts.keySet()) {
                    int loserCount = loserItemCounts.get(itemKey);
                    int winnerCount = winnerItemCounts.getOrDefault(itemKey, 0);
                    int unmatchedCount = loserCount - Math.min(loserCount, winnerCount);

                    if (unmatchedCount > 0) {
                        ItemStack unmatchedStack = findItemStackByKey(loserWager.getItems(), itemKey);
                        if (unmatchedStack != null) {
                            ItemStack returnStack = unmatchedStack.copy();
                            returnStack.setCount(unmatchedCount);
                            loserReturns.addItem(returnStack);
                        }
                    }
                }

                // Return unmatched currency
                int loserCurrency = loserWager.getCurrency();
                int winnerCurrency = winnerOriginalWager.getCurrency();
                int unmatchedCurrency = loserCurrency - Math.min(loserCurrency, winnerCurrency);
                loserReturns.setCurrency(unmatchedCurrency);
            }

            if (!loserReturns.isEmpty()) {
                distribution.put(loserUuid, loserReturns);
            }
        }

        return distribution;
    }

    /**
     * Return all wagers (used when duel is cancelled)
     */
    public Map<UUID, WagerData> returnAllWagers() {
        Map<UUID, WagerData> returns = new HashMap<>();
        for (UUID playerUuid : playerWagers.keySet()) {
            returns.put(playerUuid, playerWagers.get(playerUuid).copy());
        }
        return returns;
    }

    /**
     * Helper: Count items by type
     */
    private Map<String, Integer> getItemCounts(List<ItemStack> items) {
        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack stack : items) {
            String key = stack.getItem().toString();
            counts.put(key, counts.getOrDefault(key, 0) + stack.getCount());
        }
        return counts;
    }

    /**
     * Helper: Find an ItemStack by its item type key
     */
    private ItemStack findItemStackByKey(List<ItemStack> items, String key) {
        for (ItemStack stack : items) {
            if (stack.getItem().toString().equals(key)) {
                return stack;
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return playerWagers.isEmpty() || playerWagers.values().stream().allMatch(WagerData::isEmpty);
    }

    public Map<UUID, WagerData> getAllWagers() {
        Map<UUID, WagerData> wagers = new HashMap<>();
        for (Map.Entry<UUID, WagerData> entry : playerWagers.entrySet()) {
            wagers.put(entry.getKey(), entry.getValue().copy());
        }
        return wagers;
    }
}
