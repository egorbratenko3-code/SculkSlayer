package dev.sculkslayer.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

import dev.sculkslayer.infection.DamageRules;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	/** Hallow armor and shield effects: every point of damage passes through here on the server. */
	@WrapMethod(method = "hurtServer")
	private boolean sculkslayer$hurtServer(ServerLevel level, DamageSource source, float amount, Operation<Boolean> original) {
		float modified = DamageRules.modify((LivingEntity) (Object) this, source, amount);
		return original.call(level, source, modified);
	}
}
