package com.munsterduck.gambapvp.mixin;

import com.munsterduck.gambapvp.battle.BattleData;
import com.munsterduck.gambapvp.battle.BattleManager;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.EndCrystalItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Tracks end crystal entities placed by battle participants,
 * so their explosions can be filtered to only destroy battle-placed blocks.
 */
@Mixin(EndCrystalItem.class)
public class EndCrystalPlaceMixin {

    @Inject(method = "useOnBlock", at = @At("RETURN"))
    private void gambapvp$trackCrystalPlace(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (context.getWorld().isClient()) return;
        if (!cir.getReturnValue().isAccepted()) return;
        if (!(context.getPlayer() instanceof ServerPlayerEntity serverPlayer)) return;

        BattleData battle = BattleManager.getPlayerBattle(serverPlayer.getUuid());
        if (battle == null || !battle.isActive()) return;

        // The crystal spawns one block above the clicked block
        BlockPos crystalPos = context.getBlockPos().up();
        List<EndCrystalEntity> crystals = context.getWorld().getEntitiesByClass(
                EndCrystalEntity.class,
                new Box(crystalPos),
                e -> true
        );
        for (EndCrystalEntity crystal : crystals) {
            battle.addPlacedEntity(crystal.getUuid());
        }
    }
}
