package com.munsterduck.gambapvp.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import com.munsterduck.gambapvp.GambaPVP;

import java.util.ArrayList;
import java.util.List;

public class BattleSetupScreenHandler extends ScreenHandler {

    private final List<String> playerNames;

    // Constructor called by the client when receiving the screen handler
    public BattleSetupScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        super(GambaPVP.BATTLE_SETUP_SCREEN_HANDLER, syncId);
        // Read the player names from the packet
        this.playerNames = buf.readCollection(ArrayList::new, PacketByteBuf::readString);
    }

    // Constructor called by the server when creating the screen handler
    private BattleSetupScreenHandler(int syncId, PlayerInventory playerInventory, List<String> playerNames) {
        super(GambaPVP.BATTLE_SETUP_SCREEN_HANDLER, syncId);
        this.playerNames = playerNames;
    }

    public List<String> getPlayerNames() {
        return playerNames;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        // Handle shift-clicking items (not needed for a simple GUI)
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    // Factory class for opening the screen
    public static class Factory implements net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory {
        private final List<String> playerNames;

        public Factory(List<String> playerNames) {
            this.playerNames = playerNames;
        }

        @Override
        public Text getDisplayName() {
            return Text.literal("Battle Setup");
        }

        @Override
        public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
            return new BattleSetupScreenHandler(syncId, playerInventory, playerNames);
        }

        @Override
        public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
            // Send the list of online player names to the client
            buf.writeCollection(playerNames, PacketByteBuf::writeString);
        }
    }
}