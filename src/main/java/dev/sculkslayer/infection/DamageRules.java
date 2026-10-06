package dev.sculkslayer.infection;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import dev.sculkslayer.registry.ModItems;

/** Hallow armor effects that reduce incoming damage. */
public final class DamageRules {
	public static float modify(LivingEntity self, DamageSource source, float amount) {
		if (!(self instanceof ServerPlayer player) || amount <= 0.0F) return amount;
		Entity attacker = source.getEntity();

		if (attacker != null && attacker.getType() == EntityType.WARDEN) {
			// the Hallow shield stops every warden attack, including the sonic boom
			if (player.isBlocking() && player.getUseItem().is(ModItems.HALLOW_SHIELD)) return 0.0F;
			if (PlayerEffects.wearsChest(player)) amount *= 0.5F; // breastplate: -50% warden damage
		}
		if (attacker != null && InfectedMobs.isSculkAligned(attacker) && PlayerEffects.wearsGreaves(player)) {
			amount *= 0.9F; // greaves: -10% damage from sculk mobs
		}
		if (PlayerEffects.wearsHelmet(player)) {
			amount *= 0.95F; // helmet: -5% damage from everything
		}
		return amount;
	}

	private DamageRules() {}
}
