package com.munsterduck.gambapvp.client;

import com.munsterduck.gambapvp.network.BattleRequestPacket;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.hud.Hud;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders the battle scoreboard HUD using OWO-UI.
 */
public class BattleHudRenderer {
    private static final Identifier BATTLE_HUD_ID = new Identifier("gambapvp", "battle_hud");

    private static String currentBattleId = null;
    private static int winsRequired = 0;
    private static final Map<String, Integer> playerScores = new HashMap<>();
    private static final Map<String, String> playerNames = new HashMap<>(); // uuid -> name

    /**
     * Show the battle HUD with initial data.
     */
    public static void showHud(BattleRequestPacket.ShowBattleHud data) {
        currentBattleId = data.battleId();
        winsRequired = data.winsRequired();
        playerScores.clear();
        playerNames.clear();

        for (BattleRequestPacket.PlayerScoreData player : data.players()) {
            playerScores.put(player.playerUuid(), player.score());
            playerNames.put(player.playerUuid(), player.playerName());
        }

        // Remove existing HUD if any
        Hud.remove(BATTLE_HUD_ID);

        // Create new HUD
        Hud.add(BATTLE_HUD_ID, BattleHudRenderer::createHudComponent);
    }

    /**
     * Update a player's score on the HUD.
     */
    public static void updateScore(BattleRequestPacket.UpdateBattleScore data) {
        if (!data.battleId().equals(currentBattleId)) {
            return;
        }

        // Find player by name and update score
        for (Map.Entry<String, String> entry : playerNames.entrySet()) {
            if (entry.getValue().equals(data.playerName())) {
                playerScores.put(entry.getKey(), data.newScore());
                break;
            }
        }

        // Refresh HUD
        refreshHud();
    }

    /**
     * Hide the battle HUD.
     */
    public static void hideHud(String battleId) {
        if (battleId.equals(currentBattleId)) {
            Hud.remove(BATTLE_HUD_ID);
            currentBattleId = null;
            playerScores.clear();
            playerNames.clear();
        }
    }

    /**
     * Refresh the HUD by removing and re-adding it.
     */
    private static void refreshHud() {
        Hud.remove(BATTLE_HUD_ID);
        if (currentBattleId != null) {
            Hud.add(BATTLE_HUD_ID, BattleHudRenderer::createHudComponent);
        }
    }

    /**
     * Create the HUD component.
     */
    private static Component createHudComponent() {
        FlowLayout container = Containers.verticalFlow(Sizing.content(), Sizing.content());

        // Header: "First to X"
        container.child(
                Components.label(Text.literal("First to " + winsRequired)
                                .styled(s -> s.withColor(Formatting.GOLD).withBold(true)))
                        .horizontalTextAlignment(HorizontalAlignment.CENTER)
                        .shadow(true)
        );

        container.child(Components.box(Sizing.fixed(100), Sizing.fixed(1))
                .color(Color.ofArgb(0x88FFFFFF)));

        // Player scores
        for (Map.Entry<String, String> entry : playerNames.entrySet()) {
            String uuid = entry.getKey();
            String name = entry.getValue();
            int score = playerScores.getOrDefault(uuid, 0);

            Formatting nameColor = score >= winsRequired ? Formatting.GREEN : Formatting.WHITE;
            Formatting scoreColor = score > 0 ? Formatting.YELLOW : Formatting.GRAY;

            FlowLayout playerRow = Containers.horizontalFlow(Sizing.content(), Sizing.content());

            playerRow.child(Components.label(Text.literal(name)
                            .styled(s -> s.withColor(nameColor)))
                    .shadow(true));

            playerRow.child(Components.label(Text.literal(": ")
                            .styled(s -> s.withColor(Formatting.GRAY)))
                    .shadow(true));

            playerRow.child(Components.label(Text.literal(String.valueOf(score))
                            .styled(s -> s.withColor(scoreColor).withBold(true)))
                    .shadow(true));

            playerRow.child(Components.label(Text.literal("/" + winsRequired)
                            .styled(s -> s.withColor(Formatting.DARK_GRAY)))
                    .shadow(true));

            container.child(playerRow);
        }

        return container
                .surface(Surface.flat(0xAA000000).and(Surface.outline(0xFF333333)))
                .padding(Insets.of(8))
                .positioning(Positioning.relative(100, 50)); // Right middle
    }

    /**
     * Check if HUD is currently showing.
     */
    public static boolean isShowing() {
        return currentBattleId != null && Hud.getComponent(BATTLE_HUD_ID) != null;
    }
}
