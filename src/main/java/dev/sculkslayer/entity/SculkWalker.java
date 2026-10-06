package dev.sculkslayer.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

/** Fast sculk humanoid. Does not burn in daylight and does not drown-convert. */
public class SculkWalker extends Zombie {
	public SculkWalker(EntityType<? extends SculkWalker> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createWalkerAttributes() {
		return Zombie.createAttributes()
				.add(Attributes.MAX_HEALTH, 32.0)
				.add(Attributes.ATTACK_DAMAGE, 6.0)
				.add(Attributes.MOVEMENT_SPEED, 0.29)
				.add(Attributes.FOLLOW_RANGE, 48.0);
	}

	@Override
	protected boolean isSunSensitive() {
		return false;
	}

	@Override
	protected boolean convertsInWater() {
		return false;
	}
}
