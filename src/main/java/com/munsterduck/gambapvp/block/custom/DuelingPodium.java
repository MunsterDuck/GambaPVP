package com.munsterduck.gambapvp.block.custom;

import com.munsterduck.gambapvp.GambaPVP;
import com.munsterduck.gambapvp.network.OpenBattleScreenPacket;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DuelingPodium extends Block {
    public DuelingPodium(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        if (player instanceof ServerPlayerEntity serverPlayer) {
            OpenBattleScreenPacket.send(serverPlayer);
        }
        return ActionResult.CONSUME;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
        if (Screen.hasShiftDown()) {
                tooltip.add(Text.translatable("tooltip.gambapvp.dueling_podium.tooltip.shift"));
        } else {
            tooltip.add(Text.translatable("tooltip.gambapvp.dueling_podium.tooltip"));
        }
        super.appendTooltip(stack, world, tooltip, options);
    }
}
