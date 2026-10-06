package dev.sculkslayer.infection;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Generic melee AI injected into any infected mob: hunts the nearest player, hits with stage-scaled damage,
 * breaks weak blocks (stage 7+) and phases through sculk walls (Critical 2+).
 */
public class InfectedAttackGoal extends Goal {
	private final PathfinderMob mob;
	private int attackCooldown;
	private int breakCooldown;
	private boolean phasing;

	public InfectedAttackGoal(PathfinderMob mob) {
		this.mob = mob;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	private static int stage() {
		SculkState s = SculkManager.state();
		return s == null ? 0 : s.stage();
	}

	@Override
	public boolean canUse() {
		boolean netherHunt = mob.level().dimension() == Level.NETHER;
		if ((!InfectedMobs.isInfected(mob) && !netherHunt) || !(mob.level() instanceof ServerLevel level)) return false;
		LivingEntity t = mob.getTarget();
		if (t instanceof Player p && peacefulPiglinTarget(p)) {
			mob.setTarget(null);
			t = null;
		}
		if (t == null || !t.isAlive()) {
			Player nearest = null;
			double nearestDistance = 24.0 * 24.0;
			for (Player p : level.players()) {
				double distance = mob.distanceToSqr(p);
				if (p.isCreative() || p.isSpectator() || distance > nearestDistance || peacefulPiglinTarget(p)) continue;
				nearest = p;
				nearestDistance = distance;
			}
			if (nearest == null) return false;
			mob.setTarget(nearest);
		}
		return mob.getTarget() != null && !(mob.getTarget() instanceof Player p && peacefulPiglinTarget(p));
	}

	private boolean peacefulPiglinTarget(Player player) {
		return mob instanceof AbstractPiglin
				&& player.getItemBySlot(EquipmentSlot.HEAD).is(Items.GOLDEN_HELMET)
				&& player.getItemBySlot(EquipmentSlot.CHEST).is(Items.GOLDEN_CHESTPLATE)
				&& player.getItemBySlot(EquipmentSlot.LEGS).is(Items.GOLDEN_LEGGINGS)
				&& player.getItemBySlot(EquipmentSlot.FEET).is(Items.GOLDEN_BOOTS);
	}

	@Override
	public boolean canContinueToUse() {
		LivingEntity t = mob.getTarget();
		if (t instanceof Player p && peacefulPiglinTarget(p)) return false;
		return (InfectedMobs.isInfected(mob) || mob.level().dimension() == Level.NETHER)
				&& t != null && t.isAlive() && mob.distanceToSqr(t) < 48 * 48;
	}

	@Override
	public void stop() {
		if (mob.getTarget() instanceof Player p && peacefulPiglinTarget(p)) mob.setTarget(null);
		setPhasing(false);
		mob.getNavigation().stop();
	}

	private void setPhasing(boolean on) {
		if (phasing != on) {
			phasing = on;
			mob.noPhysics = on;
			mob.setNoGravity(on);
		}
	}

	@Override
	public void tick() {
		LivingEntity target = mob.getTarget();
		if (target == null || !(mob.level() instanceof ServerLevel level)) return;
		int stage = stage();
		mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

		boolean inSculkWall = InfectionUtil.isInfectedFamily(level.getBlockState(mob.blockPosition()))
				|| InfectionUtil.isInfectedFamily(level.getBlockState(mob.blockPosition().above()));
		Vec3 dir = target.position().subtract(mob.position());
		double dist = dir.length();
		Vec3 norm = dist > 1.0E-4 ? dir.scale(1.0 / dist) : Vec3.ZERO;

		if (stage >= Stage.C2 && dist > 1.5) {
			BlockPos ahead = BlockPos.containing(mob.position().add(norm.x, 0.6, norm.z));
			boolean wallAhead = InfectionUtil.isInfectedFamily(level.getBlockState(ahead));
			if (inSculkWall || (wallAhead && mob.getNavigation().isDone())) {
				setPhasing(true);
				mob.setDeltaMovement(norm.scale(0.055));
				return;
			}
		}
		setPhasing(false);

		mob.getNavigation().moveTo(target, 1.15);

		if (breakCooldown > 0) breakCooldown--;
		if (stage >= 7 && breakCooldown == 0 && mob.getNavigation().isDone() && dist < 14) {
			tryBreakBlock(level, dir, stage);
			breakCooldown = 30;
		}

		if (attackCooldown > 0) attackCooldown--;
		double reach = mob.getBbWidth() * 1.6 + target.getBbWidth() + 0.6;
		if (attackCooldown == 0 && mob.distanceToSqr(target) <= reach * reach) {
			attackCooldown = 20;
			mob.swing(InteractionHand.MAIN_HAND);
			float dmg = (float) ((3.0 + 0.6 * stage) * Stage.mobPower(stage));
			target.hurtServer(level, level.damageSources().mobAttack(mob), dmg);
		}
	}

	private void tryBreakBlock(ServerLevel level, Vec3 dir, int stage) {
		float limit = stage >= Stage.C2 ? 6.0F : stage >= Stage.C1 ? 3.0F : 2.0F;
		Direction d = Direction.getApproximateNearest(dir.x, 0, dir.z);
		BlockPos front = mob.blockPosition().relative(d);
		for (BlockPos p : new BlockPos[] {front, front.above()}) {
			BlockState st = level.getBlockState(p);
			if (st.isAir() || st.hasBlockEntity() || InfectionUtil.isProtected(p)) continue;
			float h = st.getDestroySpeed(level, p);
			if (h >= 0 && h <= limit) {
				level.destroyBlock(p, false, mob);
			}
		}
	}
}
