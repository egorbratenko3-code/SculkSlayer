package dev.sculkslayer.infection;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import dev.sculkslayer.SculkSlayer;
import dev.sculkslayer.mixin.MobAccessor;

/** Turns ANY mob into a sculk-infected hunter. State lives in an entity tag; AI and stats are re-applied on load. */
public final class InfectedMobs {
	public static final String TAG = "sculkslayer_infected";
	private static final String NETHER_HUNTER_TAG = "sculkslayer_nether_hunter";

	private static final Identifier HP_ID = SculkSlayer.id("infected_health");
	private static final Identifier SPEED_ID = SculkSlayer.id("infected_speed");

	public static boolean isInfected(Entity e) {
		return e.getTags().contains(TAG);
	}

	public static boolean isSculkAligned(Entity e) {
		return isInfected(e) || InfectionUtil.isSculkMobType(e);
	}

	public static boolean canInfect(Mob mob) {
		EntityType<?> t = mob.getType();
		return !isSculkAligned(mob)
				&& t != EntityType.WARDEN
				&& t != EntityType.ENDER_DRAGON
				&& t != EntityType.WITHER
				&& mob instanceof PathfinderMob;
	}

	public static void infect(ServerLevel level, Mob mob) {
		if (!canInfect(mob)) return;

		mob.addTag(TAG);
		applyStats(mob);
		mob.setHealth(mob.getMaxHealth());
		attachGoals(mob);
		mob.setGlowingTag(true);

		Scoreboard sb = level.getScoreboard();
		PlayerTeam team = sb.getPlayerTeam("sculk_infected");

		if (team == null) {
			team = sb.addPlayerTeam("sculk_infected");
			team.setColor(ChatFormatting.DARK_AQUA);
		}

		sb.addPlayerToTeam(mob.getScoreboardName(), team);

		level.sendParticles(
				net.minecraft.core.particles.ParticleTypes.SCULK_SOUL,
				mob.getX(),
				mob.getY() + 0.5,
				mob.getZ(),
				12,
				0.3,
				0.4,
				0.3,
				0.02
		);
	}

	/** Re-applies transient attribute modifiers (they are not saved by vanilla). */
	public static void applyStats(Mob mob) {
		SculkState s = SculkManager.state();
		double power = s == null ? 1.0 : Stage.mobPower(s.stage());

		AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
		if (hp != null) {
			hp.addOrUpdateTransientModifier(
					new AttributeModifier(
							HP_ID,
							power * 0.5,
							AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
					)
			);
		}

		AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.addOrUpdateTransientModifier(
					new AttributeModifier(
							SPEED_ID,
							0.2,
							AttributeModifier.Operation.ADD_MULTIPLIED_BASE
					)
			);
		}
	}

	public static void attachGoals(Mob mob) {
		if (!(mob instanceof PathfinderMob pm)) {
			return;
		}

		GoalSelector goalSelector = ((MobAccessor) pm).sculkslayer$getGoalSelector();
		goalSelector.addGoal(1, new InfectedAttackGoal(pm));
	}

	public static void attachNetherGoals(Mob mob) {
		if (mob.getTags().contains(NETHER_HUNTER_TAG) || !(mob instanceof PathfinderMob pm)) return;
		((MobAccessor) pm).sculkslayer$getGoalSelector().addGoal(1, new InfectedAttackGoal(pm));
		mob.addTag(NETHER_HUNTER_TAG);
	}

	private InfectedMobs() {}
}
