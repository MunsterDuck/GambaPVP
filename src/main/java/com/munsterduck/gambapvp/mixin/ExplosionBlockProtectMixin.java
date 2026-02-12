package com.munsterduck.gambapvp.mixin;

import com.munsterduck.gambapvp.battle.BattleData;
import com.munsterduck.gambapvp.battle.BattleManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;

/**
 * Prevents explosions caused by battle participants from destroying
 * blocks that weren't placed during the battle (protects arena structures).
 *
 * Uses two tracking strategies:
 * 1. getCausingEntity() - handles TNT ignited by a player, beds, respawn anchors
 * 2. Placed entity tracking - handles end crystals and TNT ignited by redstone/chains
 *    (tracked by EndCrystalPlaceMixin and TntEntityTrackMixin)
 */
@Mixin(Explosion.class)
public abstract class ExplosionBlockProtectMixin {

    @Shadow public abstract List<BlockPos> getAffectedBlocks();
    @Shadow @Nullable public abstract LivingEntity getCausingEntity();
    @Shadow @Final private World world;
    @Shadow @Final @Nullable private Entity entity;

    @Inject(method = "affectWorld", at = @At("HEAD"))
    private void gambapvp$filterBattleBlocks(boolean particles, CallbackInfo ci) {
        if (world.isClient()) return;

        BattleData battle = gambapvp$findBattleForExplosion();
        if (battle != null) {
            Set<BlockPos> placedBlocks = battle.getPlacedBlocks();
            getAffectedBlocks().removeIf(pos -> !placedBlocks.contains(pos));
        }
    }

    @Nullable
    private BattleData gambapvp$findBattleForExplosion() {
        // 1. Check if a battle player directly caused this explosion (TNT lit by player, beds, etc.)
        LivingEntity causingEntity = this.getCausingEntity();
        if (causingEntity instanceof ServerPlayerEntity player) {
            BattleData battle = BattleManager.getPlayerBattle(player.getUuid());
            if (battle != null && battle.isActive()) {
                return battle;
            }
        }

        // 2. Check if the explosion's source entity was placed/tracked during a battle
        //    (end crystals via EndCrystalPlaceMixin, TNT via TntEntityTrackMixin)
        if (entity != null) {
            BattleData battle = BattleManager.findBattleByPlacedEntity(entity.getUuid());
            if (battle != null) {
                return battle;
            }
        }

        return null;
    }
}
