package dev.sculkslayer.infection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import dev.sculkslayer.registry.ModBlocks;
import dev.sculkslayer.registry.ModEntities;
import dev.sculkslayer.registry.ModItems;

/** Wires every Fabric event the mod listens to. */
public final class SculkEvents {
	private static boolean swordGuard;
	private static final Map<UUID, List<ItemStack>> STASH = new HashMap<>();

	/** Forgets per-world data on server stop (static state would otherwise leak into the next world). */
	public static void reset() {
		swordGuard = false;
		STASH.clear();
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(SculkManager::load);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> SculkManager.unload());
		ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> SculkManager.save());
		ServerTickEvents.END_SERVER_TICK.register(SculkManager::tick);
		// this is how the infection front reaches an unbounded area for free: only chunks that actually load are touched
		ServerChunkEvents.CHUNK_LOAD.register(SculkManager::onChunkLoad);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer p = handler.getPlayer();
			SculkManager.syncTo(p);
			SculkState st = SculkManager.state();
			if (st != null && !st.active) {
				p.displayClientMessage(Component.literal("Sculk Slayer: type /sculkslayer start to raise the monolith and begin."), false);
			}
		});

		// infected mobs lose their AI when chunks reload, so re-attach it
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof Mob mob && InfectedMobs.isInfected(mob)) {
				InfectedMobs.applyStats(mob);
				InfectedMobs.attachGoals(mob);
			} else if (entity instanceof Mob mob && mob.level().dimension() == net.minecraft.world.level.Level.NETHER) {
				InfectedMobs.attachNetherGoals(mob);
			}
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			SculkManager.onDeath(entity, source);
			if (entity instanceof ServerPlayer p && p.level() instanceof ServerLevel level) {
				// the Hallow tools never stay on the ground: they return with the player
				for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(8.0),
						i -> i.getItem().is(ModItems.HALLOW_SWORD) || i.getItem().is(ModItems.HALLOW_PICKAXE) || i.getItem().is(ModItems.HALLOW_AXE))) {
					ie.discard();
				}
			}
		});

		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer p) {
				List<ItemStack> keep = new ArrayList<>();
				for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
					ItemStack s = p.getInventory().getItem(i);
								if (s.is(ModItems.HALLOW_SWORD) || s.is(ModItems.HALLOW_PICKAXE) || s.is(ModItems.HALLOW_AXE)) keep.add(s.copy());
				}
				if (keep.isEmpty()) STASH.remove(p.getUUID()); else STASH.put(p.getUUID(), keep);
			}
			return true;
		});

		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			if (alive) return;
			List<ItemStack> stash = STASH.remove(oldPlayer.getUUID());
			if (stash != null) {
				for (ItemStack s : stash) giveOrDrop(newPlayer, s);
			}
		});

		// Hallow sword: extra damage equal to 10% of the target's max health
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (swordGuard || blocked) return;
			if (!(source.getEntity() instanceof ServerPlayer p) || source.getDirectEntity() != p) return;
			if (!p.getMainHandItem().is(ModItems.HALLOW_SWORD)) return;
			if (!(entity.level() instanceof ServerLevel level)) return;
			EntityType<?> t = entity.getType();
			if (t == EntityType.ENDER_DRAGON || t == EntityType.WITHER) return;
			if (!(entity instanceof Enemy) && !InfectedMobs.isInfected(entity)) return;
			swordGuard = true;
			try {
				entity.invulnerableTime = 0;
				entity.hurtServer(level, level.damageSources().playerAttack(p), entity.getMaxHealth() * 0.10F);
			} finally {
				swordGuard = false;
			}
		});

		// Hallow pickaxe never hurts creatures
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
				player.getItemInHand(hand).is(ModItems.HALLOW_PICKAXE) ? InteractionResult.FAIL : InteractionResult.PASS);

		// monolith signs hand out the tools
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (level.isClientSide() || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
			SculkState st = SculkManager.state();
			if (st == null || !st.active) return InteractionResult.PASS;
			BlockPos pos = hit.getBlockPos();
			if (pos.equals(st.signSword)) {
				if (st.stage() < 3) {
					player.displayClientMessage(Component.literal("The Hallow Sword has not awakened yet. It answers only from stage 3 of the infection."), true);
					return InteractionResult.SUCCESS;
				}
				grant(player, ModItems.HALLOW_SWORD, st.gotSword, "Hallow Sword");
				return InteractionResult.SUCCESS;
			}
			if (pos.equals(st.signPick)) {
				if (st.stage() < 5) {
					player.displayClientMessage(Component.literal("The Hallow Pickaxe has not awakened yet. It answers only from stage 5 of the infection."), true);
					return InteractionResult.SUCCESS;
				}
				grant(player, ModItems.HALLOW_PICKAXE, st.gotPick, "Hallow Pickaxe");
				return InteractionResult.SUCCESS;
			}
			if (pos.equals(st.signAxe)) {
				if (st.stage() < 7) {
					player.displayClientMessage(Component.literal("The Hallow Axe has not awakened yet. It answers only from stage 7 of the infection."), true);
					return InteractionResult.SUCCESS;
				}
				grant(player, ModItems.HALLOW_AXE, st.gotAxe, "Hallow Axe");
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});

		// the Sculk Core: only a weakened core, only the Hallow pickaxe
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, be) -> {
			if (!state.is(ModBlocks.SCULK_CORE)) return true;
			SculkState st = SculkManager.state();
			boolean ok = st != null && st.coreWeak && player.getMainHandItem().is(ModItems.HALLOW_PICKAXE);
			if (!ok) {
				player.displayClientMessage(Component.literal(st != null && !st.coreWeak
						? "The core is too strong. Weaken it with a Holy Grenade first."
						: "Only the Hallow Pickaxe can break the core."), true);
			}
			return ok;
		});
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
			if (!(level instanceof ServerLevel sl)) return;
			if (state.is(ModBlocks.SCULK_CORE)) {
				SculkManager.finishWorld();
				return;
			}
			SculkState st = SculkManager.state();
			if (st != null && st.active && st.stage() >= 6 && !st.cleansed
					&& (state.is(ModBlocks.TAINTED_BLOCK) || state.is(ModBlocks.DECAYED_BLOCK) || state.is(ModBlocks.SCULK_NEST))
					&& sl.random.nextFloat() < 0.25F) {
				SculkManager.spawn(sl, ModEntities.PARASITE, pos); // parasites hiding inside the block escape
			}
		});
	}

	private static boolean hasItem(Player p, Item item) {
		for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
			if (p.getInventory().getItem(i).is(item)) return true;
		}
		return false;
	}

	private static void grant(Player player, Item item, Set<UUID> owners, String name) {
		if (hasItem(player, item)) {
			player.displayClientMessage(Component.literal("You already carry the " + name + "."), true);
			return;
		}
		giveOrDrop(player, new ItemStack(item));
		owners.add(player.getUUID());
		player.displayClientMessage(Component.literal("You received the " + name + ". It will always return to you."), true);
	}

	private static void giveOrDrop(Player p, ItemStack stack) {
		if (!p.getInventory().add(stack)) {
			p.drop(stack, false);
		}
	}

	private SculkEvents() {}
}
