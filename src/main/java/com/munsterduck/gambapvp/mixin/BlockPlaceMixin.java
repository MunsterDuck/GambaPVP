package com.munsterduck.gambapvp.mixin;

import com.munsterduck.gambapvp.battle.BattleData;
import com.munsterduck.gambapvp.battle.BattleManager;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockPlaceMixin {

    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
            at = @At("RETURN"))
    private void onBlockPlace(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (context.getWorld().isClient()) {
            return;
        }

        if (!(context.getPlayer() instanceof ServerPlayerEntity player)) {
            return;
        }

        // Check if placement was successful
        if (cir.getReturnValue() != ActionResult.SUCCESS &&
            cir.getReturnValue() != ActionResult.CONSUME) {
            return;
        }

        // Check if player is in a battle
        BattleData battle = BattleManager.getPlayerBattle(player.getUuid());
        if (battle != null && battle.isActive()) {
            // Track the placed block
            battle.addPlacedBlock(context.getBlockPos());
        }
    }
}
