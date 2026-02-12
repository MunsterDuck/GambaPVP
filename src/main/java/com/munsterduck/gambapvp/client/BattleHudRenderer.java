package com.munsterduck.gambapvp.client;

import com.munsterduck.gambapvp.network.BattleRequestPacket;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.hud.Hud;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class BattleHudRenderer {
    private static final Identifier BATTLE_HUD_ID = new Identifier("gambapvp", "battle_hud");

    private static boolean active = false;
    private static int winsRequired = 0;
    private static final Map<String, Integer> playerScores = new HashMap<>();
    private static final Map<String, String> playerNames = new HashMap<>();

    public static void showHud(BattleRequestPacket.ShowBattleHud data) {
        winsRequired = data.winsRequired();
        playerScores.clear();
        playerNames.clear();

        for (BattleRequestPacket.PlayerScoreData player : data.players()) {
            playerScores.put(player.playerUuid(), player.score());
            playerNames.put(player.playerUuid(), player.playerName());
        }

        active = true;
        refresh();
    }

    public static void updateScore(BattleRequestPacket.UpdateBattleScore data) {
        if (!active) return;

        for (Map.Entry<String, String> entry : playerNames.entrySet()) {
            if (entry.getValue().equals(data.playerName())) {
                playerScores.put(entry.getKey(), data.newScore());
                break;
            }
        }

        refresh();
    }

    public static void hideHud(String battleId) {
        active = false;
        playerScores.clear();
        playerNames.clear();
        Hud.remove(BATTLE_HUD_ID);
    }

    private static void refresh() {
        Hud.remove(BATTLE_HUD_ID);
        Hud.add(BATTLE_HUD_ID, BattleHudRenderer::createHudComponent);
    }

    private static Component createHudComponent() {
        // When not active, return a zero-size invisible component (must have positioning for OWO HUD)
        if (!active || playerNames.isEmpty()) {
            return Containers.verticalFlow(Sizing.fixed(0), Sizing.fixed(0))
                    .positioning(Positioning.relative(100, 50));
        }

        FlowLayout outer = Containers.verticalFlow(Sizing.content(), Sizing.content());

        // Header
        outer.child(
                Components.label(Text.literal("\u2694 DUEL \u2694").styled(s -> s.withColor(0xFFAA00).withBold(true)))
                        .horizontalTextAlignment(HorizontalAlignment.CENTER)
                        .shadow(true)
        );

        // Player scores
        for (Map.Entry<String, String> entry : playerNames.entrySet()) {
            String uuid = entry.getKey();
            String name = entry.getValue();
            int score = playerScores.getOrDefault(uuid, 0);
            boolean isWinner = score >= winsRequired;

            FlowLayout row = Containers.horizontalFlow(Sizing.content(), Sizing.content());
            row.gap(3);

            int nameColor = isWinner ? 0x55FF55 : 0xAAAAAA;
            row.child(
                    Components.label(Text.literal(name).styled(s -> s.withColor(nameColor)))
                            .shadow(true)
            );

            for (int i = 0; i < winsRequired; i++) {
                boolean filled = i < score;
                int pipColor = filled ? (isWinner ? 0x55FF55 : 0xFFAA00) : 0x444444;
                row.child(
                        Components.label(Text.literal("\u2B24").styled(s -> s.withColor(pipColor)))
                                .shadow(true)
                );
            }

            outer.child(row);
        }

        return outer
                .surface(Surface.flat(0xAA000000).and(Surface.outline(0xFF333333)))
                .padding(Insets.of(4, 5, 4, 5))
                .positioning(Positioning.relative(100, 50));
    }

    public static boolean isShowing() {
        return active;
    }
}
