package com.munsterduck.gambapvp.mixin;

import com.munsterduck.gambapvp.battle.BattleData;
import com.munsterduck.gambapvp.battle.BattleDeathHandler;
import com.munsterduck.gambapvp.battle.BattleManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class BattleDeathPreventMixin {

    /**
     * Intercept damage for players in battle.
     * - Cancel all damage during spawn immunity
     * - Cancel lethal damage and handle as "fake death"
     */
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }

        BattleData battle = BattleManager.getPlayerBattle(player.getUuid());
        if (battle == null || !battle.isActive()) {
            return;
        }

        // Block all damage during spawn immunity
        if (battle.hasSpawnImmunity(player.getUuid())) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }

        // Block all damage during countdown (battle hasn't started yet)
        if (!battle.isCountdownComplete()) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }

        // Check if this damage would kill the player
        float healthAfterDamage = player.getHealth() - amount;
        if (healthAfterDamage <= 0) {
            // Prevent the actual death - handle it ourselves
            cir.setReturnValue(false);
            cir.cancel();

            // Handle the "fake death" on the server thread
            player.getServer().execute(() -> {
                BattleDeathHandler.handleBattleDeath(player, source, battle);
            });
        }
    }
}
