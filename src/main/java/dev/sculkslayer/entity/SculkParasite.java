package dev.sculkslayer.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.Level;

import dev.sculkslayer.infection.ParasiteLogic;
import dev.sculkslayer.registry.ModEntities;

/** Small sculk worm. Hits hard, can burrow into players and mobs, and matures into a Sculk Walker. */
public class SculkParasite extends Silverfish {
	public SculkParasite(EntityType<? extends SculkParasite> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createParasiteAttributes() {
		return Silverfish.createAttributes()
				.add(Attributes.MAX_HEALTH, 12.0)
				.add(Attributes.ATTACK_DAMAGE, 5.0)
				.add(Attributes.MOVEMENT_SPEED, 0.32)
				.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && target instanceof LivingEntity living) {
			ParasiteLogic.onParasiteHit(level, this, living);
		}
		return hit;
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel sl && tickCount > 2400 && tickCount % 40 == 0 && random.nextInt(12) == 0) {
			if (sl.getEntitiesOfClass(SculkWalker.class, getBoundingBox().inflate(24.0)).size() < 6) {
				SculkWalker walker = ModEntities.WALKER.create(sl, EntitySpawnReason.TRIGGERED);
				if (walker != null) {
					walker.setPos(getX(), getY(), getZ());
					sl.addFreshEntity(walker);
					discard();
				}
			}
		}
	}
}
