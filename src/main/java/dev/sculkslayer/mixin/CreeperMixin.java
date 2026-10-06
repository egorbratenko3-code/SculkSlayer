package dev.sculkslayer.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.sculkslayer.entity.SculkCreeper;
import dev.sculkslayer.infection.SculkManager;
import dev.sculkslayer.infection.SculkState;

@Mixin(Creeper.class)
public abstract class CreeperMixin {
	@Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
	private void sculkslayer$seedInfectionBlast(CallbackInfo ci) {
		if (!((Object) this instanceof SculkCreeper creeper)) return;
		if (!(creeper.level() instanceof ServerLevel server)) {
			ci.cancel();
			return;
		}
		BlockPos impact = creeper.blockPosition();
		server.explode(creeper, creeper.getX(), creeper.getY(), creeper.getZ(), 3.0F, Level.ExplosionInteraction.MOB);
		SculkState state = SculkManager.state();
		int stage = state == null ? 5 : Math.max(5, state.stage());
		SculkManager.infectAround(server, impact, 14, 4, stage);
		creeper.discard();
		ci.cancel();
	}
}
