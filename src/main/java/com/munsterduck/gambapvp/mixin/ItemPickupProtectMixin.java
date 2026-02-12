package com.munsterduck.gambapvp.mixin;

import com.munsterduck.gambapvp.battle.BattleData;
import com.munsterduck.gambapvp.battle.BattleManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Protects item entities during battles:
 * - Non-battle players cannot pick up battle items
 * - Battle players cannot pick up non-battle items
 * - Battle explosions cannot destroy non-battle items on the ground
 */
@Mixin(ItemEntity.class)
public abstract class ItemPickupProtectMixin {

    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void gambapvp$protectBattlePickup(PlayerEntity player, CallbackInfo ci) {
        if (player.getWorld().isClient()) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        Entity self = (Entity) (Object) this;
        BattleData itemBattle = BattleManager.findBattleByPlacedEntity(self.getUuid());
        BattleData playerBattle = BattleManager.getPlayerBattle(serverPlayer.getUuid());

        // Battle player trying to pick up non-battle item
        if (playerBattle != null && playerBattle.isActive() && itemBattle == null) {
            ci.cancel();
            return;
        }

        // Non-battle player (or different battle) trying to pick up battle item
        if (itemBattle != null && (playerBattle == null || !playerBattle.getBattleId().equals(itemBattle.getBattleId()))) {
            ci.cancel();
        }
    }

    /**
     * Prevent battle explosions from destroying non-battle items on the ground.
     * Checks if the damage source is from a battle participant or tracked battle entity,
     * and if so, only allows damage to items tracked in that same battle.
     */
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void gambapvp$protectFromBattleExplosion(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self.getWorld().isClient()) return;

        // Find if this damage originates from a battle
        BattleData explosionBattle = null;

        // Check if a battle player caused this (TNT lit by player, bed, etc.)
        Entity attacker = source.getAttacker();
        if (attacker instanceof ServerPlayerEntity player) {
            BattleData b = BattleManager.getPlayerBattle(player.getUuid());
            if (b != null && b.isActive()) explosionBattle = b;
        }

        // Check if a tracked battle entity caused this (end crystal, chain TNT)
        if (explosionBattle == null) {
            Entity sourceEntity = source.getSource();
            if (sourceEntity != null) {
                explosionBattle = BattleManager.findBattleByPlacedEntity(sourceEntity.getUuid());
            }
        }

        if (explosionBattle == null) return; // Not battle-related damage, allow normally

        // If this item is NOT tracked in the explosion's battle, protect it
        BattleData itemBattle = BattleManager.findBattleByPlacedEntity(self.getUuid());
        if (itemBattle == null || !itemBattle.getBattleId().equals(explosionBattle.getBattleId())) {
            cir.setReturnValue(false);
        }
    }
}
