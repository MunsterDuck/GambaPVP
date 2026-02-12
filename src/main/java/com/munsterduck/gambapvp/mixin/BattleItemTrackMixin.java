package com.munsterduck.gambapvp.mixin;

import com.munsterduck.gambapvp.battle.BattleData;
import com.munsterduck.gambapvp.battle.BattleManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tracks item entities that spawn from battle-related actions:
 * - Items from breaking/exploding placed blocks (spawn at placed block position)
 * - Items dropped by battle participants (spawn at player position)
 *
 * Tracked items are protected from non-battle pickup and cleaned up at battle end.
 */
@Mixin(ServerWorld.class)
public abstract class BattleItemTrackMixin {

    @Inject(method = "spawnEntity", at = @At("HEAD"))
    private void gambapvp$trackBattleItems(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof ItemEntity)) return;

        BlockPos pos = entity.getBlockPos();

        // Check if item spawns at a placed block position (block break / explosion drops)
        BattleData battle = BattleManager.findBattleByPlacedBlock(pos);
        if (battle != null) {
            battle.addPlacedEntity(entity.getUuid());
            return;
        }

        // Check if item spawns at a battle participant's position (player drops / death)
        ServerWorld self = (ServerWorld) (Object) this;
        for (ServerPlayerEntity player : self.getPlayers(p -> BattleManager.isInBattle(p.getUuid()))) {
            BlockPos playerPos = player.getBlockPos();
            // Items from drops spawn at player's X/Z, Y varies by ~1 block (eye height)
            if (playerPos.getX() == pos.getX() && playerPos.getZ() == pos.getZ()
                    && Math.abs(playerPos.getY() - pos.getY()) <= 1) {
                BattleData playerBattle = BattleManager.getPlayerBattle(player.getUuid());
                if (playerBattle != null && playerBattle.isActive()) {
                    playerBattle.addPlacedEntity(entity.getUuid());
                    return;
                }
            }
        }
    }
}
