package dev.sculkslayer.infection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Everything holy: cleansing blocks, smiting the unholy, blessing players. Big purges run as budgeted jobs. */
public final class HolyEffects {
	private static final class Job {
		final ServerLevel level;
		final BlockPos center;
		final int radius;
		final long total;
		long index;

		Job(ServerLevel level, BlockPos center, int radius) {
			this.level = level;
			this.center = center;
			this.radius = radius;
			long side = 2L * radius + 1;
			this.total = side * side * side;
		}
	}

	private static final List<Job> JOBS = new ArrayList<>();
	private static final int BLOCKS_PER_TICK = 15000;

	// ------------------------------------------------------------------ public entry points

	public static void holyWater(ServerLevel level, BlockPos c, LivingEntity owner) {
		purgeNow(level, c, 4, false);
		smite(level, c, 5.0, false, 30.0F);
		bless(level, c, 5.0, 30);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, c.getX() + 0.5, c.getY() + 1.0, c.getZ() + 0.5, 40, 1.5, 0.8, 1.5, 0.05);
		level.sendParticles(ParticleTypes.END_ROD, c.getX() + 0.5, c.getY() + 1.0, c.getZ() + 0.5, 25, 1.2, 1.0, 1.2, 0.08);
		level.playSound(null, c, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.4F, 0.8F);
	}

	public static void crucifix(ServerLevel level, BlockPos c, int radius, Player user) {
		queuePurge(level, c, radius);
		smite(level, c, radius, true, 0.0F);
		bless(level, c, radius / 2.0, 20);
		level.sendParticles(ParticleTypes.END_ROD, c.getX() + 0.5, c.getY() + 1.5, c.getZ() + 0.5, 200, 6.0, 3.0, 6.0, 0.15);
		level.playSound(null, c, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 0.7F);
		if (user instanceof ServerPlayer sp) {
			sp.displayClientMessage(Component.literal("The crucifix burns with holy light!"), true);
		}
	}

	public static void grenade(ServerLevel level, BlockPos c, LivingEntity owner) {
		queuePurge(level, c, 32);
		smite(level, c, 32.0, true, 0.0F);
		// blast: every hostile creature near the impact is hurt and thrown back
		AABB box = new AABB(c).inflate(8.0);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> x instanceof Enemy)) {
			SculkManager.holyKill = true;
			try {
				e.hurtServer(level, level.damageSources().magic(), 30.0F);
			} finally {
				SculkManager.holyKill = false;
			}
			e.knockback(1.6, c.getX() + 0.5 - e.getX(), c.getZ() + 0.5 - e.getZ());
		}
		bless(level, c, 10.0, 10);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5, 3, 1.0, 0.5, 1.0, 0.0);
		level.sendParticles(ParticleTypes.END_ROD, c.getX() + 0.5, c.getY() + 1.0, c.getZ() + 0.5, 300, 5.0, 3.0, 5.0, 0.3);
		level.playSound(null, c, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 4.0F, 1.4F);
		SculkManager.onGrenade(level, c);
	}

	/** Cleanses a small sphere immediately. Returns the number of blocks cleansed. */
	public static int purgeNow(ServerLevel level, BlockPos c, int r, boolean smiteMobs) {
		int cleaned = 0;
		for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -r, -r), c.offset(r, r, r))) {
			if (p.distSqr(c) > (double) r * r + 1) continue;
			if (cleanseAt(level, p)) cleaned++;
		}
		if (cleaned > 0) {
			SculkState s = SculkManager.state();
			if (s != null) s.addInfection(-Stage.PURGE_LOSS * cleaned);
		}
		if (smiteMobs) smite(level, c, r + 1, false, 20.0F);
		return cleaned;
	}

	public static void queuePurge(ServerLevel level, BlockPos c, int radius) {
		JOBS.add(new Job(level, c.immutable(), radius));
	}

	// ------------------------------------------------------------------ internals

	static boolean cleanseAt(ServerLevel level, BlockPos pos) {
		BlockState st = level.getBlockState(pos);
		if (!InfectionUtil.isInfectedFamily(st)) return false;
		level.setBlock(pos, InfectionUtil.cleanState(level, pos, st), Block.UPDATE_CLIENTS);
		return true;
	}

	public static boolean isEvil(LivingEntity e) {
		return !(e instanceof Player)
				&& (InfectedMobs.isSculkAligned(e) || e.getType() == EntityType.WARDEN || e.getType().is(EntityTypeTags.UNDEAD));
	}

	static void smite(ServerLevel level, BlockPos c, double radius, boolean instantKill, float damage) {
		AABB box = new AABB(c).inflate(radius);
		double r2 = radius * radius;
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, HolyEffects::isEvil)) {
			if (e.distanceToSqr(c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5) > r2) continue;
			SculkManager.holyKill = true;
			try {
				if (instantKill) {
					e.hurtServer(level, level.damageSources().genericKill(), 100000.0F);
				} else {
					float dmg = e.getType() == EntityType.WARDEN ? damage * 2 : damage;
					e.hurtServer(level, level.damageSources().magic(), dmg);
				}
			} finally {
				SculkManager.holyKill = false;
			}
			level.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + 1.0, e.getZ(), 8, 0.3, 0.5, 0.3, 0.05);
		}
	}

	static void bless(ServerLevel level, BlockPos c, double radius, int seconds) {
		SculkState s = SculkManager.state();
		for (ServerPlayer p : level.players()) {
			if (p.distanceToSqr(c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5) > radius * radius) continue;
			int t = seconds * 20;
			p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, t, 2));
			p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, t, 2));
			p.addEffect(new MobEffectInstance(MobEffects.SATURATION, 20, 0));
			if (s != null && s.parasitized.remove(p.getUUID())) {
				p.displayClientMessage(Component.literal("The parasites inside you burn away."), true);
			}
		}
	}

	/** Drops queued jobs; they hold references to levels of a world that may no longer exist. */
	public static void clearJobs() {
		JOBS.clear();
	}

	/** Called every server tick (even when the plague is inactive) to advance queued purges. */
	public static void tickJobs() {
		if (JOBS.isEmpty()) return;
		int budget = BLOCKS_PER_TICK;
		SculkState s = SculkManager.state();
		Iterator<Job> it = JOBS.iterator();
		while (it.hasNext() && budget > 0) {
			Job j = it.next();
			long side = 2L * j.radius + 1;
			int cleaned = 0;
			int r2 = j.radius * j.radius;
			while (j.index < j.total && budget-- > 0) {
				long i = j.index++;
				int dx = (int) (i % side) - j.radius;
				int dz = (int) ((i / side) % side) - j.radius;
				int dy = (int) (i / (side * side)) - j.radius;
				if (dx * dx + dy * dy + dz * dz > r2) continue;
				BlockPos p = j.center.offset(dx, dy, dz);
				if (!SculkManager.loaded(j.level, p)) continue;
				if (cleanseAt(j.level, p)) cleaned++;
			}
			if (cleaned > 0 && s != null) s.addInfection(-Stage.PURGE_LOSS * cleaned);
			if (j.index >= j.total) it.remove();
		}
	}

	private HolyEffects() {}
}
