package dev.sculkslayer.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

/** Huge, slow and very tanky. Appears in the late stages and around the Sculk Core. */
public class SculkBrute extends Zombie {
	public SculkBrute(EntityType<? extends SculkBrute> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createBruteAttributes() {
		return Zombie.createAttributes()
				.add(Attributes.MAX_HEALTH, 90.0)
				.add(Attributes.ATTACK_DAMAGE, 11.0)
				.add(Attributes.MOVEMENT_SPEED, 0.22)
				.add(Attributes.ARMOR, 8.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
				.add(Attributes.SCALE, 1.4)
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
