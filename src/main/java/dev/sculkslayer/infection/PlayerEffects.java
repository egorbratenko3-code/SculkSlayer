package dev.sculkslayer.infection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import dev.sculkslayer.SculkSlayer;
import dev.sculkslayer.block.AltarMenu;
import dev.sculkslayer.registry.ModBlocks;
import dev.sculkslayer.registry.ModEntities;
import dev.sculkslayer.registry.ModItems;

/** Everything the plague and the Hallow gear do to individual players. */
public final class PlayerEffects {
	private static final Identifier BOOTS_SPEED = SculkSlayer.id("hallow_boots_speed");
	private static final Map<UUID, Boolean> WAS_BLOCKING = new HashMap<>();
	private static final Map<UUID, Long> DASH_COOLDOWN = new HashMap<>();
	private static final int DASH_COOLDOWN_TICKS = 100; // 5 seconds

	public static boolean wears(Player p, EquipmentSlot slot, Item item) {
		return p.getItemBySlot(slot).is(item);
	}
	public static boolean wearsHelmet(Player p) { return wears(p, EquipmentSlot.HEAD, ModItems.HALLOW_HELMET); }
	public static boolean wearsChest(Player p) { return wears(p, EquipmentSlot.CHEST, ModItems.HALLOW_BREASTPLATE); }
	public static boolean wearsGreaves(Player p) { return wears(p, EquipmentSlot.LEGS, ModItems.HALLOW_GREAVES); }
	public static boolean wearsBoots(Player p) { return wears(p, EquipmentSlot.FEET, ModItems.HALLOW_BOOTS); }

	/** Forgets per-world data. The maps are static, so without this they leak from one world into the next. */
	public static void reset() {
		WAS_BLOCKING.clear();
		DASH_COOLDOWN.clear();
	}

	public static void tick(MinecraftServer server, SculkState st) {
		long now = st.ticks;
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (p.isSpectator()) continue;
			if (now % 10 == 0) bootsSpeed(p);
			shieldDash(p, now);
			altarGuard(p);
			if (now % 20 == 0 && p.level() instanceof ServerLevel level) {
				limitHallowTools(p);
				if (!st.cleansed) {
					if (!p.isCreative()) sculkDamage(level, p, st);
					parasiteTick(level, p, st, now);
					if (!p.isCreative() && !p.isSpectator()) atmosphere(level, p, st, now);
				}
			}
		}
	}

	// ------------------------------------------------------------------ boots

	private static void bootsSpeed(ServerPlayer p) {
		AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) return;
		if (wearsBoots(p)) {
			speed.addOrUpdateTransientModifier(new AttributeModifier(BOOTS_SPEED, 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		} else if (speed.getModifier(BOOTS_SPEED) != null) {
			speed.removeModifier(BOOTS_SPEED);
		}
	}

	// ------------------------------------------------------------------ shield

	private static void shieldDash(ServerPlayer p, long now) {
		UUID id = p.getUUID();
		boolean blocking = p.isBlocking() && p.getUseItem().is(ModItems.HALLOW_SHIELD);
		boolean was = WAS_BLOCKING.getOrDefault(id, false);
		WAS_BLOCKING.put(id, blocking);
		if (!blocking || was) return;
		long last = DASH_COOLDOWN.getOrDefault(id, -10000L);
		if (now - last < DASH_COOLDOWN_TICKS) return;
		DASH_COOLDOWN.put(id, now);
		p.getCooldowns().addCooldown(SculkSlayer.id("hallow_shield"), DASH_COOLDOWN_TICKS);
		dash(p);
	}

	private static void dash(ServerPlayer p) {
		if (!(p.level() instanceof ServerLevel level)) return;
		Vec3 look = p.getLookAngle();
		Vec3 dir = new Vec3(look.x, 0.0, look.z);
		if (dir.lengthSqr() < 1.0E-4) return;
		dir = dir.normalize();
		p.setDeltaMovement(dir.x * 1.3, 0.2, dir.z * 1.3);
		p.hurtMarked = true;

		AABB box = p.getBoundingBox().inflate(2.5, 0.6, 2.5).move(dir.scale(1.6));
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
				x -> x != p && x.isAlive() && (x instanceof Enemy || HolyEffects.isEvil(x)))) {
			// bonus damage against sculk and nether creatures
			float dmg = (InfectedMobs.isSculkAligned(e) || e.fireImmune() || e.getType() == EntityType.WARDEN) ? 12.0F : 5.0F;
			SculkManager.holyKill = true;
			try {
				e.hurtServer(level, level.damageSources().playerAttack(p), dmg);
			} finally {
				SculkManager.holyKill = false;
			}
			e.knockback(1.0, p.getX() - e.getX(), p.getZ() - e.getZ());
		}
		level.sendParticles(ParticleTypes.CRIT, p.getX(), p.getY() + 1.0, p.getZ(), 25, 0.6, 0.5, 0.6, 0.2);
		level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F, 0.7F);
	}

	// ------------------------------------------------------------------ altar-only crafting

	/** Holy items may only be crafted at the Hallow Altar: blank the result in any other crafting menu. */
	private static void altarGuard(ServerPlayer p) {
		if (p.containerMenu instanceof CraftingMenu menu) {
			Slot result = menu.getSlot(0);
			ItemStack out = result.getItem();
			if (out.isEmpty()) return;
			SculkState state = SculkManager.state();
			if (out.is(ModItems.HALLOW_AXE) && (state == null || state.stage() < 7)) {
				result.set(ItemStack.EMPTY);
				menu.broadcastChanges();
				p.displayClientMessage(Component.literal("The Hallow Axe recipe awakens at infection stage 7."), true);
				return;
			}
			if (!(menu instanceof AltarMenu) && isAltarOnly(out.getItem())) {
				result.set(ItemStack.EMPTY);
				menu.broadcastChanges();
			}
		}
	}

	/** Hallow bars, blocks, wood and the altar itself are crafted anywhere; everything holy needs the altar. */
	public static boolean isAltarOnly(Item item) {
		return item == ModItems.CRUCIFIX || item == ModItems.HOLY_WATER || item == ModItems.HOLY_GRENADE
				|| item == ModItems.SACRAMENT || item == ModItems.HALLOW_HELMET || item == ModItems.HALLOW_BREASTPLATE
				|| item == ModItems.HALLOW_GREAVES || item == ModItems.HALLOW_BOOTS || item == ModItems.HALLOW_SHIELD
				|| item == ModItems.HALLOW_AXE;
	}

	// ------------------------------------------------------------------ one sword / one pickaxe

	private static void limitHallowTools(ServerPlayer p) {
		boolean sword = false;
		boolean pick = false;
		for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
			ItemStack s = p.getInventory().getItem(i);
			if (s.is(ModItems.HALLOW_SWORD)) {
				if (sword) p.getInventory().setItem(i, ItemStack.EMPTY);
				sword = true;
			} else if (s.is(ModItems.HALLOW_PICKAXE)) {
				if (pick) p.getInventory().setItem(i, ItemStack.EMPTY);
				pick = true;
			}
		}
	}

	// ------------------------------------------------------------------ plague damage

	// ------------------------------------------------------------------ atmosphere / dread

	/** Deep in infected territory the world should feel hostile: darkness pulses and distant, dreadful sounds. */
	private static void atmosphere(ServerLevel level, ServerPlayer p, SculkState st, long now) {
		int stage = st.stage();
		if (stage < 4) return;
		BlockPos pos = p.blockPosition();
		int infected = 0;
		for (BlockPos q : BlockPos.betweenClosed(pos.offset(-5, -3, -5), pos.offset(5, 3, 5))) {
			if (InfectionUtil.isInfectedFamily(level.getBlockState(q))) {
				infected++;
				if (infected >= 10) break;
			}
		}
		if (infected < 6) return; // only when genuinely surrounded, not just next to a stray patch

		if (now % 300 == 0 && level.random.nextFloat() < 0.15F + 0.03F * stage) {
			p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0, true, false));
		}
		if (stage >= 5 && now % 400 == 0 && level.random.nextFloat() < 0.05F + 0.02F * (stage - 4)) {
			// a distant, muffled roar from somewhere in the dark - honest dread, since wardens really do lurk this deep
			level.playSound(null, pos, SoundEvents.WARDEN_ROAR, SoundSource.AMBIENT, 0.5F, 0.6F + level.random.nextFloat() * 0.3F);
		}
	}

	private static void sculkDamage(ServerLevel level, ServerPlayer p, SculkState st) {
		int stage = st.stage();
		float reduction = wearsBoots(p) ? 0.5F : 1.0F;
		BlockPos feet = p.blockPosition();

		// crystals cut anyone who touches them
		AABB bb = p.getBoundingBox().inflate(0.05);
		boolean cut = false;
		for (BlockPos q : BlockPos.betweenClosed(BlockPos.containing(bb.minX, bb.minY, bb.minZ), BlockPos.containing(bb.maxX, bb.maxY, bb.maxZ))) {
			if (level.getBlockState(q).is(ModBlocks.SCULK_CRYSTAL)) { cut = true; break; }
		}
		if (!cut && level.getBlockState(feet.below()).is(ModBlocks.SCULK_CRYSTAL_BLOCK)) cut = true;
		if (cut) {
			p.hurtServer(level, level.damageSources().magic(), 2.0F * reduction);
			return;
		}

		if (stage >= 6 && (InfectionUtil.isInfectedFamily(level.getBlockState(feet.below()))
				|| InfectionUtil.isInfectedFamily(level.getBlockState(feet)))) {
			p.hurtServer(level, level.damageSources().magic(), (1.0F + 0.5F * (stage - 6)) * reduction);
			return;
		}
		if (stage >= Stage.C1) {
			for (BlockPos q : BlockPos.betweenClosed(feet.offset(-3, -3, -3), feet.offset(3, 3, 3))) {
				if (InfectionUtil.isInfectedFamily(level.getBlockState(q))) {
					p.hurtServer(level, level.damageSources().magic(), 0.5F * (stage - 7) * reduction);
					return;
				}
			}
		}
	}

	// ------------------------------------------------------------------ parasites inside players

	private static void parasiteTick(ServerLevel level, ServerPlayer p, SculkState st, long now) {
		if (!st.parasitized.contains(p.getUUID())) return;
		// NOTE: used to also apply Weakness here. Weakness subtracts a flat 4 attack damage, which is exactly the
		// Hallow Sword's whole base damage (tier bonus 0 + sword bonus 3 + fist 1) - so a parasitized player's every
		// hit, including with their holy sword, landed for 0 damage the entire time they carried the infection.
		// Dropped the debuff so the curse stays a real threat (bleed-out damage, spawning more parasites) without
		// silently disabling the player's main weapon.
		if (now % 600 == 0 && level.random.nextFloat() < 0.5F) {
			p.displayClientMessage(Component.literal("The parasites burst out of you!"), false);
			p.hurtServer(level, level.damageSources().magic(), 4.0F);
			SculkManager.infectAround(level, p.blockPosition(), 4, 3, st.stage());
			SculkManager.spawn(level, ModEntities.PARASITE, p.blockPosition());
		}
	}

	public static void onPlayerDeath(ServerLevel level, ServerPlayer p) {
		SculkState st = SculkManager.state();
		if (st == null || !st.parasitized.remove(p.getUUID())) return;
		SculkManager.infectAround(level, p.blockPosition(), 8, 4, st.stage());
		for (int i = 0; i < 3; i++) SculkManager.spawn(level, ModEntities.PARASITE, p.blockPosition());
	}

	private PlayerEffects() {}
}
