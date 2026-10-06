package dev.sculkslayer.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

import dev.sculkslayer.infection.HolyEffects;
import dev.sculkslayer.registry.ModItems;

/** Thrown Holy Water / Holy Grenade. The carried item decides what happens on impact. */
public class HolyProjectile extends ThrowableItemProjectile {
	public HolyProjectile(EntityType<? extends HolyProjectile> type, Level level) {
		super(type, level);
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.HOLY_WATER;
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (level() instanceof ServerLevel sl && !isRemoved()) {
			BlockPos pos = BlockPos.containing(hit.getLocation());
			LivingEntity owner = getOwner() instanceof LivingEntity le ? le : null;
			if (getItem().is(ModItems.HOLY_GRENADE)) {
				HolyEffects.grenade(sl, pos, owner);
			} else {
				HolyEffects.holyWater(sl, pos, owner);
			}
			discard();
		}
	}
}
