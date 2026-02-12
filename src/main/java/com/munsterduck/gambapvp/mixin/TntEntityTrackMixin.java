package com.munsterduck.gambapvp.mixin;

import com.munsterduck.gambapvp.battle.BattleData;
import com.munsterduck.gambapvp.battle.BattleManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tracks TNT entities that spawn at battle-placed block positions.
 * This covers TNT ignited by redstone or explosion chains where
 * getCausingEntity() would return null.
 */
@Mixin(TntEntity.class)
public abstract class TntEntityTrackMixin {

    @Inject(method = "<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/entity/LivingEntity;)V",
            at = @At("RETURN"))
    private void gambapvp$trackBattleTnt(World world, double x, double y, double z,
                                          @Nullable LivingEntity igniter, CallbackInfo ci) {
        if (world.isClient()) return;

        // Check if this TNT was at a battle-placed block position
        BlockPos blockPos = BlockPos.ofFloored(x, y, z);
        BattleData battle = BattleManager.findBattleByPlacedBlock(blockPos);
        if (battle != null) {
            Entity self = (Entity) (Object) this;
            battle.addPlacedEntity(self.getUuid());
        }
    }
}
