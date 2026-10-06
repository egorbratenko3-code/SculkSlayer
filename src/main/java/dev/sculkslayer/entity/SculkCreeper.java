package dev.sculkslayer.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

/** A late-stage Creeper whose blast leaves a living sculk scar. */
public class SculkCreeper extends Creeper {
	public SculkCreeper(EntityType<? extends SculkCreeper> type, Level level) {
		super(type, level);
		setGlowingTag(true);
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide() && tickCount % 8 == 0) {
			((ServerLevel) level()).sendParticles(net.minecraft.core.particles.ParticleTypes.SCULK_SOUL,
					getX(), getY() + 0.8, getZ(), 2, 0.2, 0.3, 0.2, 0.01);
		}
	}

	public static AttributeSupplier.Builder createSculkCreeperAttributes() {
		return Creeper.createAttributes();
	}

}
