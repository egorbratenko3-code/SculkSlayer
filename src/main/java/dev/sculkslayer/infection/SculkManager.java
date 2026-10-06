package dev.sculkslayer.infection;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;

import dev.sculkslayer.SculkSlayer;
import dev.sculkslayer.block.SculkHoardBlockEntity;
import dev.sculkslayer.entity.HolyProjectile;
import dev.sculkslayer.entity.SculkBrute;
import dev.sculkslayer.entity.SculkParasite;
import dev.sculkslayer.entity.SculkWalker;
import dev.sculkslayer.registry.ModBlocks;
import dev.sculkslayer.registry.ModEntities;
import dev.sculkslayer.registry.ModItems;

/** The brain of the plague: stage progression, spreading, spawning and the end-game core. */
public final class SculkManager {
	private static SculkState state;
	private static MinecraftServer server;
	private static final RandomSource RNG = RandomSource.create();
	private static double spreadAcc;
	private static int autoTimer;

	// ---- deferred chunk seeding -----------------------------------------------------------
	// CHUNK_LOAD fires while the chunk is still being promoted to FULL. Touching the world from inside that event
	// (getHeight / getBlockState / setBlock -> level.getChunk) makes the server thread wait for the very chunk it is
	// finishing: chunks keep streaming in, but no tick ever runs and the world can neither be left nor re-entered.
	// So the event only remembers the chunk; the actual seeding happens later, from the normal server tick.
	private static final ArrayDeque<ChunkPos> PENDING_CHUNKS = new ArrayDeque<>();
	private static final LongOpenHashSet QUEUED_CHUNKS = new LongOpenHashSet();
	private static final int MAX_PENDING_CHUNKS = 8192;
	private static final int CHUNKS_PER_TICK = 2;
	private static final long SEED_BUDGET_NANOS = 4_000_000L;
	private static final long SPREAD_BUDGET_NANOS = 10_000_000L;

	// ---- ambient (pre-plague) sculk -------------------------------------------------------
	// Independent of the monolith/front system: a sparse background presence that makes the world feel like the
	// plague has quietly existed for a long time even before the monolith is ever built, or in overworld chunks
	// the active front hasn't reached yet. Uses the exact same "queue on chunk load, seed a couple per tick inside
	// a tiny time budget" shape as the plague front above, so it costs nothing for chunks nobody ever visits and
	// never spends more than a sliver of a tick even in chunks that do load.
	private static final ArrayDeque<ChunkPos> AMBIENT_PENDING = new ArrayDeque<>();
	private static final LongOpenHashSet AMBIENT_QUEUED = new LongOpenHashSet();
	/** Session-only, like QUEUED_CHUNKS's cousin below: not persisted, so an old world may reseed a chunk once more after a restart. */
	private static final LongOpenHashSet AMBIENT_SEEDED = new LongOpenHashSet();
	private static final int MAX_AMBIENT_PENDING = 8192;
	private static final int AMBIENT_CHUNKS_PER_TICK = 2;
	private static final long AMBIENT_SEED_BUDGET_NANOS = 2_000_000L;
	private static final int MAX_AMBIENT_SEEDED = 3_000_000;
	private static final double AMBIENT_SURFACE_CHANCE = 0.22;
	private static final double AMBIENT_CAVE_CHANCE = 0.28;

	/** True while a holy source is killing something, so the death does not feed the plague. */
	public static boolean holyKill;

	public static SculkState state() { return state; }
	public static MinecraftServer server() { return server; }

	// ------------------------------------------------------------------ lifecycle

	private static Path file(MinecraftServer s) {
		return s.getWorldPath(LevelResource.ROOT).resolve("sculkslayer_state.json");
	}

	public static void load(MinecraftServer s) {
		server = s;
		state = SculkState.load(file(s));
		spreadAcc = 0;
		autoTimer = 0;
		PENDING_CHUNKS.clear();
		QUEUED_CHUNKS.clear();
		AMBIENT_PENDING.clear();
		AMBIENT_QUEUED.clear();
		AMBIENT_SEEDED.clear();
	}

	public static void save() {
		if (state != null && server != null) state.save(file(server));
	}

	public static void unload() {
		save();
		HolyEffects.clearJobs();
		PENDING_CHUNKS.clear();
		QUEUED_CHUNKS.clear();
		AMBIENT_PENDING.clear();
		AMBIENT_QUEUED.clear();
		AMBIENT_SEEDED.clear();
		PlayerEffects.reset();
		SculkEvents.reset();
		spreadAcc = 0;
		autoTimer = 0;
		state = null;
		server = null;
	}

	/**
	 * Non-blocking "is this position in a fully loaded chunk". Unlike the vanilla hasChunkAt it never waits for a
	 * chunk that is still being generated / promoted, so it is safe to call from anywhere on the server thread.
	 */
	public static boolean loaded(ServerLevel level, BlockPos pos) {
		return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
	}

	public static String dimKey(ServerLevel level) {
		return level.dimension().identifier().toString();
	}

	public static ServerLevel levelOf(String key) {
		for (ServerLevel l : server.getAllLevels()) {
			if (dimKey(l).equals(key)) return l;
		}
		return null;
	}

	public static ServerLevel overworld() {
		return server.getLevel(Level.OVERWORLD);
	}

	// ------------------------------------------------------------------ main tick

	public static void tick(MinecraftServer s) {
		if (state == null) return;
		HolyEffects.tickJobs();
		// ambient sculk keeps growing quietly everywhere, whether or not the plague itself has been unleashed yet
		processAmbientQueue();
		if (!state.active) {
			autoStartCheck();
			return;
		}
		ServerLevel sanctuaryLevel = s.getLevel(Level.OVERWORLD);
		if (sanctuaryLevel != null) MonolithBuilder.patchExisting(sanctuaryLevel, state);
		state.ticks++;
		if (state.cleansed) {
			PlayerEffects.tick(s, state);
			cleansingTick();
			return;
		}

		processChunkQueue();

		int stage = state.stage();
		if (state.ticks % 20 == 0) {
			state.addInfection(Stage.timeGain(stage));
			int ns = state.stage();
			if (ns != state.lastStage) {
				announceStage(ns);
				state.lastStage = ns;
			}
			int cap = Stage.radiusCap(ns);
			if (state.frontRadius < cap) {
				double speed = Stage.radiusSpeed(ns);
				// the front actively chases the furthest player in its dimension instead of crawling at a fixed
				// pace: outrunning it only buys time, not permanent safety, once the plague can reach that far at all
				double farthest = farthestPlayerDistance();
				if (farthest > state.frontRadius) {
					speed += Math.min(300.0, farthest - state.frontRadius) * 0.015;
				}
				state.frontRadius = Math.min(cap, state.frontRadius + speed);
			}
			if (ns >= Stage.C3 && state.core == null) trySpawnCore();
			syncAll();
		}

		boolean frenzy = state.core != null && state.coreWeak;
		double rate = Stage.spreadPerTick(stage) * (frenzy ? 3.0 : 1.0);
		if (stage >= 5) {
			// the plague feeds on its own size once it's established: growth compounds instead of staying flat,
			// so a large, mature infection cascades outward far faster than a young one at the same stage
			int tracked = 0;
			for (LongArrayList l : state.nodes.values()) tracked += l.size();
			rate *= 1.0 + Math.min(3.5, tracked / 3500.0);
		}
		spreadAcc += rate;
		int stepCap = stage >= 7 ? 60 : stage >= 5 ? 40 : 12;
		int steps = 0;
		long spreadStart = System.nanoTime();
		while (spreadAcc >= 1.0 && steps < stepCap) {
			spreadAcc -= 1.0;
			steps++;
			spreadStep();
			// never let the plague eat the whole tick: TPS matters more than one extra block of growth
			if ((steps & 7) == 0 && System.nanoTime() - spreadStart > SPREAD_BUDGET_NANOS) break;
		}
		if (spreadAcc > stepCap) spreadAcc = 0;

		PlayerEffects.tick(s, state);

		if (state.ticks % 40 == 0) mobScan(stage);
		if (state.ticks % 100 == 0) naturalSpawns(stage);
		if (state.core != null && state.ticks % 20 == 0) coreTick(stage);
		if (state.ticks % 6000 == 0) save();
	}

	// ------------------------------------------------------------------ starting

	/** First time a world is played with the mod, the monolith rises by itself a few seconds after a player arrives. */
	private static void autoStartCheck() {
		if (state.autoStarted || state.cleansed) return;
		if (++autoTimer % 20 != 0) return;
		ServerLevel ow = overworld();
		if (ow == null) return;
		for (ServerPlayer p : ow.players()) {
			if (p.isSpectator() || p.tickCount < 100) continue;
			startAt(p);
			return;
		}
	}

	/** Builds the monolith ~20 blocks in front of the player and starts the plague. */
	public static boolean startAt(ServerPlayer player) {
		if (state == null || !(player.level() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return false;
		state.autoStarted = true;
		net.minecraft.world.phys.Vec3 look = player.getLookAngle();
		net.minecraft.world.phys.Vec3 dir = new net.minecraft.world.phys.Vec3(look.x, 0, look.z);
		dir = dir.lengthSqr() < 1.0E-4 ? new net.minecraft.world.phys.Vec3(0, 0, 1) : dir.normalize();
		BlockPos origin = BlockPos.containing(player.getX() + dir.x * 20, player.getY(), player.getZ() + dir.z * 20);
		MonolithBuilder.build(level, origin, state);
		state.infection = 0;
		state.lastStage = 0;
		// the chunks right around the monolith are already loaded (the player is standing there), so the normal
		// chunk-load seeding would never touch them; plant a real, growing patch immediately so the plague is
		// visible from the first second instead of waiting for the front radius to reach still-unloaded chunks
		state.frontRadius = 30;
		infectAround(level, state.monolith, 50, 22, 0);
		save();
		syncAll();
		BlockPos m = state.monolith;
		broadcast("The monolith has risen at " + m.getX() + " " + m.getY() + " " + m.getZ()
				+ ". Click its signs for the Hallow Sword and Pickaxe; the Hallow Axe awakens at stage 7. The plague begins.");
		return true;
	}

	// ------------------------------------------------------------------ spreading

	static void addNode(ServerLevel level, BlockPos pos) {
		LongArrayList list = state.nodesOf(dimKey(level));
		if (list.size() >= SculkState.MAX_NODES) {
			list.set(RNG.nextInt(list.size()), pos.asLong());
		} else {
			list.add(pos.asLong());
		}
	}

	/** Furthest online, non-spectator player from the monolith, in the plague's own dimension. Used to make the front chase fleeing players. */
	private static double farthestPlayerDistance() {
		if (state.monolith == null) return 0.0;
		ServerLevel level = levelOf(state.dim);
		if (level == null) return 0.0;
		double max = 0.0;
		for (ServerPlayer p : level.players()) {
			if (p.isSpectator()) continue;
			double dx = p.getX() - state.monolith.getX();
			double dz = p.getZ() - state.monolith.getZ();
			double d = Math.sqrt(dx * dx + dz * dz);
			if (d > max) max = d;
		}
		return max;
	}

	private static void spreadStep() {
		if (state.nodes.isEmpty()) return;
		List<Map.Entry<String, LongArrayList>> entries = new ArrayList<>(state.nodes.entrySet());
		Map.Entry<String, LongArrayList> entry = entries.get(RNG.nextInt(entries.size()));
		LongArrayList list = entry.getValue();
		if (list.isEmpty()) return;
		ServerLevel level = levelOf(entry.getKey());
		if (level == null) return;
		if (level.dimension() == Level.NETHER) { list.clear(); return; }

		int idx = RNG.nextInt(list.size());
		BlockPos p = BlockPos.of(list.getLong(idx));
		if (!loaded(level, p)) return;
		BlockState st = level.getBlockState(p);
		Block b = st.getBlock();
		boolean solidInfected = b == ModBlocks.TAINTED_BLOCK || b == ModBlocks.DECAYED_BLOCK || b == Blocks.SCULK
				|| b == ModBlocks.SCULK_CRYSTAL_BLOCK || b == ModBlocks.SCULK_NEST;
		if (!solidInfected) {
			list.set(idx, list.getLong(list.size() - 1));
			list.removeLong(list.size() - 1);
			return;
		}

		int stage = state.stage();
		// 1. rot progression: tainted -> decayed -> sculk
		if (b == ModBlocks.TAINTED_BLOCK && RNG.nextFloat() < 0.3F) {
			level.setBlock(p, ModBlocks.DECAYED_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
			b = ModBlocks.DECAYED_BLOCK;
		} else if (b == ModBlocks.DECAYED_BLOCK && RNG.nextFloat() < 0.3F) {
			InfectionUtil.infect(level, p, Blocks.SCULK.defaultBlockState());
			b = Blocks.SCULK;
			decorateTop(level, p, stage);
		}

		// 2. infect neighbours (more attempts at higher stages; underground spread is far more aggressive than surface
		// from stage 4 onward, so caves and future nest sites fill in much faster than the open air above)
		boolean underground = !level.canSeeSky(p);
		int attempts = stage >= 7 ? 4 : stage >= 5 ? 3 : stage >= 3 ? 2 : 1;
		if (underground && stage >= 4) attempts += stage >= 7 ? 3 : 2;
		for (int attempt = 0; attempt < attempts; attempt++) {
		Direction d;
		double bias = underground
				? (stage >= 6 ? 0.80 : stage >= 4 ? 0.60 : stage >= 2 ? 0.35 : 0.15)
				: (stage >= 6 ? 0.55 : stage >= 4 ? 0.3 : stage >= 2 ? 0.12 : 0.0);
		if (RNG.nextDouble() < bias) {
			d = RNG.nextBoolean() ? Direction.DOWN : Direction.Plane.HORIZONTAL.getRandomDirection(RNG);
		} else {
			d = Direction.getRandom(RNG);
		}
		BlockPos n = p.relative(d);
		if (loaded(level, n)) {
			BlockState ns = level.getBlockState(n);
			if (InfectionUtil.isInfectible(level, n, ns)) {
				InfectionUtil.infect(level, n, ModBlocks.TAINTED_BLOCK.defaultBlockState());
				addNode(level, n);
				state.addInfection(Stage.blockGain(stage));
			} else if (stage >= 4 && ns.getFluidState().is(FluidTags.WATER) && ns.getFluidState().isSource()
					&& RNG.nextFloat() < 0.04F + 0.025F * (stage - 4)) {
				// The infection consumes a water source as a dormant sculk node, then pushes outward from the submerged patch.
				InfectionUtil.infect(level, n, Blocks.SCULK.defaultBlockState());
				addNode(level, n);
				state.addInfection(Stage.blockGain(stage));
			} else if (ns.isAir()) {
				// 3. crystals sprout from decayed and sculk blocks (small natural groups from stage 4); sculk blocks also grow veins
				double factor = b == Blocks.SCULK ? 1.0 : b == ModBlocks.DECAYED_BLOCK ? 0.3 : 0.0;
				if (stage >= 3 && RNG.nextDouble() < Stage.crystalChance(stage) * factor) {
					placeCrystal(level, n, d);
					if (stage >= 4 && RNG.nextFloat() < 0.35F) {
						// let the crystal grow into a small group instead of staying alone
						Direction d2 = Direction.getRandom(RNG);
						BlockPos n2 = p.relative(d2);
						if (d2 != d && loaded(level, n2) && level.getBlockState(n2).isAir() && !InfectionUtil.isProtected(n2)) {
							placeCrystal(level, n2, d2);
						}
					}
				} else if (b == Blocks.SCULK && RNG.nextFloat() < 0.15F) {
					placeVein(level, n, d.getOpposite());
				}
			}
		}

		}

		// 4. enough crystals around a block turn it into a crystal block
		if (stage >= 4 && (b == Blocks.SCULK || b == ModBlocks.DECAYED_BLOCK) && RNG.nextFloat() < 0.2F) {
			int crystals = 0;
			for (Direction dir : Direction.values()) {
				BlockPos cn = p.relative(dir);
				if (loaded(level, cn) && level.getBlockState(cn).is(ModBlocks.SCULK_CRYSTAL)) crystals++;
			}
			if (crystals >= 4) {
				level.setBlock(p, ModBlocks.SCULK_CRYSTAL_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}

		// 5. prune fully "dead" interior nodes (mature sculk with no remaining air or infectible neighbour at all),
		// so the tracked frontier stays biased toward blocks that can still produce new growth instead of filling
		// up with cells that will never do anything again
		if (b == Blocks.SCULK) {
			boolean alive = false;
			for (Direction dir : Direction.values()) {
				BlockPos n2 = p.relative(dir);
				if (!loaded(level, n2)) { alive = true; break; }
				BlockState n2s = level.getBlockState(n2);
				if (n2s.isAir() || InfectionUtil.isInfectible(level, n2, n2s)) { alive = true; break; }
			}
			if (!alive && idx < list.size() && list.getLong(idx) == p.asLong()) {
				list.set(idx, list.getLong(list.size() - 1));
				list.removeLong(list.size() - 1);
			}
		}
	}

	static void placeVein(ServerLevel level, BlockPos airPos, Direction towardSupport) {
		if (!loaded(level, airPos) || !level.getBlockState(airPos).isAir() || InfectionUtil.isProtected(airPos)) return;
		level.setBlock(airPos, Blocks.SCULK_VEIN.defaultBlockState()
				.setValue(MultifaceBlock.getFaceProperty(towardSupport), true), Block.UPDATE_CLIENTS);
	}

	static void placeCrystal(ServerLevel level, BlockPos airPos, Direction facing) {
		if (!loaded(level, airPos) || !level.getBlockState(airPos).isAir() || InfectionUtil.isProtected(airPos)) return;
		level.setBlock(airPos, ModBlocks.SCULK_CRYSTAL.defaultBlockState()
				.setValue(BlockStateProperties.FACING, facing), Block.UPDATE_CLIENTS);
	}

	/** Sensors / shriekers / catalysts appear on top of sculk as the stages advance; from stage 3, so does vegetation. */
	static void decorateTop(ServerLevel level, BlockPos sculk, int stage) {
		BlockPos up = sculk.above();
		boolean upAir = level.getBlockState(up).isAir() && !InfectionUtil.isProtected(up);
		if (upAir) {
			float r = RNG.nextFloat();
			BlockState put = null;
			if (stage >= 3 && r < 0.02F) put = Blocks.SCULK_CATALYST.defaultBlockState();
			else if (stage >= 2 && r < 0.05F) put = Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, true);
			else if (stage >= 1 && r < 0.13F) put = Blocks.SCULK_SENSOR.defaultBlockState();
			if (put != null) level.setBlock(up, put, Block.UPDATE_CLIENTS);
		}
		if (stage >= 3) decorateFoliage(level, sculk, stage);
	}

	/**
	 * Stage 3-4 decorative vegetation: grass, mushrooms, roots, tendrils, hanging vines, stalks, pebbles/debris.
	 * Purely cosmetic (see {@link dev.sculkslayer.block.SculkFoliageBlock}), so it is safe to roll cheaply and
	 * often instead of running any kind of growth simulation on these blocks later.
	 */
	private static void decorateFoliage(ServerLevel level, BlockPos sculk, int stage) {
		boolean rich = stage >= 4;
		BlockPos up = sculk.above();
		if (level.getBlockState(up).isAir() && !InfectionUtil.isProtected(up)) {
			float r = RNG.nextFloat();
			float mul = rich ? 1.6F : 1.0F;
			if (r < 0.09F * mul) {
				Block mush = !rich ? ModBlocks.SCULK_MUSHROOM_SMALL
						: RNG.nextFloat() < 0.5F ? ModBlocks.SCULK_MUSHROOM_MEDIUM
						: RNG.nextFloat() < 0.5F ? ModBlocks.SCULK_MUSHROOM_LARGE : ModBlocks.SCULK_MUSHROOM_SMALL;
				level.setBlock(up, mush.defaultBlockState(), Block.UPDATE_CLIENTS);
			} else if (r < 0.28F * mul) {
				level.setBlock(up, ModBlocks.SCULK_GRASS.defaultBlockState(), Block.UPDATE_CLIENTS);
			} else if (r < 0.35F * mul) {
				level.setBlock(up, ModBlocks.SCULK_LEAVES.defaultBlockState(), Block.UPDATE_CLIENTS);
			} else if (r < 0.42F * mul) {
				level.setBlock(up, (RNG.nextBoolean() ? ModBlocks.SCULK_PEBBLES : ModBlocks.SCULK_DEBRIS).defaultBlockState(), Block.UPDATE_CLIENTS);
			} else if (rich && r < 0.47F * mul) {
				level.setBlock(up, ModBlocks.SCULK_STALK.defaultBlockState(), Block.UPDATE_CLIENTS);
				BlockPos up2 = up.above();
				if (RNG.nextFloat() < 0.5F && level.getBlockState(up2).isAir()) {
					level.setBlock(up2, ModBlocks.SCULK_STALK.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
		}
		// roots, tendrils and hanging vines sprout off any exposed face of the block itself
		float chance = rich ? 0.09F : 0.045F;
		for (Direction d : Direction.values()) {
			if (RNG.nextFloat() > chance) continue;
			BlockPos n = sculk.relative(d);
			if (!loaded(level, n) || !level.getBlockState(n).isAir() || InfectionUtil.isProtected(n)) continue;
			Block growth = d == Direction.DOWN && RNG.nextFloat() < 0.5F ? ModBlocks.SCULK_VINES
					: RNG.nextBoolean() ? ModBlocks.SCULK_ROOTS : ModBlocks.SCULK_TENDRIL;
			placeGrowth(level, growth, n, d);
		}
	}

	private static void placeGrowth(ServerLevel level, Block block, BlockPos airPos, Direction facing) {
		if (!loaded(level, airPos) || !level.getBlockState(airPos).isAir() || InfectionUtil.isProtected(airPos)) return;
		level.setBlock(airPos, block.defaultBlockState().setValue(BlockStateProperties.FACING, facing), Block.UPDATE_CLIENTS);
	}

	// ------------------------------------------------------------------ the infection front (macro territory)

	/**
	 * Called for every chunk that loads. This is the only place the plague touches an area the player has never
	 * been near, so an unvisited 5000x5000 territory costs nothing: nothing happens until a chunk actually loads.
	 * A chunk inside the (noisy, non-circular) front gets a permanent one-time seeding of sculk terrain, both on
	 * the surface and underground, whose density depends on how deep inside old territory the chunk is.
	 *
	 * <p>This method runs INSIDE the chunk-load event, so it must not touch the world at all: it only queues the
	 * chunk. The seeding itself is done by {@link #processChunkQueue()} from the server tick.
	 */
	public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
		if (level.dimension() == Level.NETHER) return;
		// IMPORTANT: no world access in here (see PENDING_CHUNKS). Only cheap bookkeeping.
		SculkState st = state;
		ChunkPos cp = chunk.getPos();
		long key = cp.toLong();

		// ambient background sculk: independent of the monolith/front, only in the overworld, only once per chunk
		if (st != null && "minecraft:overworld".equals(dimKey(level)) && !AMBIENT_SEEDED.contains(key)
				&& !AMBIENT_QUEUED.contains(key) && AMBIENT_PENDING.size() < MAX_AMBIENT_PENDING
				&& AMBIENT_SEEDED.size() < MAX_AMBIENT_SEEDED) {
			AMBIENT_QUEUED.add(key);
			AMBIENT_PENDING.add(cp);
		}

		if (st == null || !st.active || st.cleansed || st.monolith == null) return;
		if (st.frontRadius < 1.0 || !dimKey(level).equals(st.dim)) return;
		if (st.seededChunks.size() >= SculkState.MAX_SEEDED_CHUNKS) return;

		if (st.seededChunks.contains(key) || QUEUED_CHUNKS.contains(key)) return;
		if (PENDING_CHUNKS.size() >= MAX_PENDING_CHUNKS) return;
		QUEUED_CHUNKS.add(key);
		PENDING_CHUNKS.add(cp);
	}

	/** Seeds a few queued ambient chunks per tick, inside a tiny time budget. See the AMBIENT_* fields above. */
	private static void processAmbientQueue() {
		if (AMBIENT_PENDING.isEmpty()) return;
		if (state == null) { AMBIENT_PENDING.clear(); AMBIENT_QUEUED.clear(); return; }
		ServerLevel level = overworld();
		if (level == null) return;

		long start = System.nanoTime();
		int done = 0;
		while (!AMBIENT_PENDING.isEmpty() && done < AMBIENT_CHUNKS_PER_TICK && System.nanoTime() - start < AMBIENT_SEED_BUDGET_NANOS) {
			ChunkPos cp = AMBIENT_PENDING.poll();
			AMBIENT_QUEUED.remove(cp.toLong());
			done++;
			if (!loaded(level, new BlockPos(cp.getMinBlockX(), level.getMinY(), cp.getMinBlockZ()))) continue;
			try {
				seedAmbientChunk(level, cp);
			} catch (RuntimeException e) {
				SculkSlayer.LOGGER.error("Failed to ambient-seed chunk {}", cp, e);
			}
		}
	}

	/**
	 * The world feels like sculk has always quietly been there: a small, deterministic (world-seed based) chance
	 * of a barely-buried patch on the surface and/or a pocket in a nearby cave, in ANY overworld chunk, whether or
	 * not the plague has ever been unleashed. Chunks the plague's own front has already seeded densely are skipped
	 * here, since that generation is already far heavier than this ambient pass would ever produce.
	 */
	private static void seedAmbientChunk(ServerLevel level, ChunkPos cp) {
		if (level.dimension() == Level.NETHER) return;
		long key = cp.toLong();
		if (AMBIENT_SEEDED.contains(key)) return;
		AMBIENT_SEEDED.add(key);
		if (state.active && state.dim.equals(dimKey(level)) && state.seededChunks.contains(key)) return;

		RandomSource r = RandomSource.create(level.getSeed() ^ (key * 0x9E3779B97F4A7C15L) ^ 0xC2B2AE3D27D4EB4FL);
		if (r.nextDouble() < AMBIENT_SURFACE_CHANCE) ambientSurfacePatch(level, cp, r);
		if (r.nextDouble() < AMBIENT_CAVE_CHANCE) ambientCavePatch(level, cp, r);
	}

	/**
	 * A tiny, slightly-buried natural patch near the surface: mostly tainted, rarely full sculk. Purely cosmetic and
	 * static - unlike every other place sculk gets placed, this deliberately does NOT register nodes for
	 * {@link #spreadStep()} to pick up. It is meant to be a quiet, ambient "this has always been here" texture, not
	 * an extra source of active growth, so it must never make the global infection percentage climb any faster.
	 */
	private static void ambientSurfacePatch(ServerLevel level, ChunkPos cp, RandomSource r) {
		int x = cp.getMinBlockX() + r.nextInt(16);
		int z = cp.getMinBlockZ() + r.nextInt(16);
		int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
		int depth = 1 + r.nextInt(3);
		BlockPos cur = new BlockPos(x, top - depth, z);
		if (!loaded(level, cur)) return;
		int size = 2 + r.nextInt(4);
		for (int i = 0; i < size; i++) {
			if (!loaded(level, cur)) break;
			BlockState cs = level.getBlockState(cur);
			if (InfectionUtil.isInfectible(level, cur, cs)) {
				float roll = r.nextFloat();
				Block put = roll < 0.55F ? ModBlocks.TAINTED_BLOCK : roll < 0.88F ? ModBlocks.DECAYED_BLOCK : Blocks.SCULK;
				level.setBlock(cur, put.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
			Direction d = r.nextFloat() < 0.35F ? Direction.DOWN : Direction.Plane.HORIZONTAL.getRandomDirection(r);
			cur = cur.relative(d);
		}
	}

	/** Same idea, but in a nearby cave air pocket instead of just under the surface. Also static: see {@link #ambientSurfacePatch}. */
	private static void ambientCavePatch(ServerLevel level, ChunkPos cp, RandomSource r) {
		int minY = level.getMinY();
		int maxY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				cp.getMinBlockX() + 8, cp.getMinBlockZ() + 8) - 6;
		if (maxY <= minY) return;
		for (int tryY = 0; tryY < 6; tryY++) {
			int x = cp.getMinBlockX() + r.nextInt(16);
			int z = cp.getMinBlockZ() + r.nextInt(16);
			int y = minY + r.nextInt(Math.max(1, maxY - minY));
			BlockPos air = new BlockPos(x, y, z);
			if (!loaded(level, air) || !level.getBlockState(air).isAir() || level.canSeeSky(air)) continue;
			BlockPos floor = air.below();
			if (!InfectionUtil.isInfectible(level, floor, level.getBlockState(floor))) continue;
			int size = 2 + r.nextInt(3);
			BlockPos cur = floor;
			for (int i = 0; i < size; i++) {
				if (!loaded(level, cur)) break;
				BlockState cs = level.getBlockState(cur);
				if (InfectionUtil.isInfectible(level, cur, cs)) {
					float roll = r.nextFloat();
					Block put = roll < 0.6F ? ModBlocks.TAINTED_BLOCK : roll < 0.9F ? ModBlocks.DECAYED_BLOCK : Blocks.SCULK;
					level.setBlock(cur, put.defaultBlockState(), Block.UPDATE_CLIENTS);
					if (put != ModBlocks.TAINTED_BLOCK && r.nextFloat() < 0.2F) placeVein(level, cur.above(), Direction.DOWN);
				}
				Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(r);
				cur = cur.relative(d);
			}
			return;
		}
	}

	/** Seeds a few queued chunks per tick, inside a small time budget. Runs from the normal server tick, where the world is safe to touch. */
	private static void processChunkQueue() {
		if (PENDING_CHUNKS.isEmpty()) return;
		if (state == null || !state.active || state.cleansed || state.monolith == null) {
			PENDING_CHUNKS.clear();
			QUEUED_CHUNKS.clear();
			return;
		}
		ServerLevel level = levelOf(state.dim);
		if (level == null) return;

		long start = System.nanoTime();
		int done = 0;
		while (!PENDING_CHUNKS.isEmpty() && done < CHUNKS_PER_TICK && System.nanoTime() - start < SEED_BUDGET_NANOS) {
			ChunkPos cp = PENDING_CHUNKS.poll();
			QUEUED_CHUNKS.remove(cp.toLong());
			done++;
			// the chunk may have unloaded again while it was waiting; it will be queued anew if it loads later
			if (!loaded(level, new BlockPos(cp.getMinBlockX(), level.getMinY(), cp.getMinBlockZ()))) continue;
			try {
				seedChunk(level, cp);
			} catch (RuntimeException e) {
				SculkSlayer.LOGGER.error("Failed to seed chunk {}", cp, e);
			}
		}
	}

	private static void seedChunk(ServerLevel level, ChunkPos cp) {
		if (level.dimension() == Level.NETHER) return;
		long key = cp.toLong();
		if (state.seededChunks.contains(key)) return;

		double cx = cp.getMiddleBlockX();
		double cz = cp.getMiddleBlockZ();
		double dx = cx - state.monolith.getX();
		double dz = cz - state.monolith.getZ();
		double dist = Math.sqrt(dx * dx + dz * dz);

		// organic, non-circular edge: the effective radius in this direction is jittered by a stable hash of angle
		double angle = Math.atan2(dz, dx);
		int bucket = (int) Math.floor((angle + Math.PI) / (2 * Math.PI) * 24.0);
		double jitter = 0.72 + 0.56 * ((Math.sin(bucket * 12.9898) * 43758.5453) % 1.0 + 1.0) % 1.0;
		double localRadius = state.frontRadius * jitter;

		int stage = state.stage();
		double depth;
		if (dist <= localRadius) {
			depth = Math.max(0.0, Math.min(1.0, (localRadius - dist) / Math.max(1.0, localRadius)));
		} else if (stage >= 5) {
			// thin tendrils reach well beyond the actual front once the plague is established: running far ahead of
			// it only delays the inevitable, it doesn't grant permanent safety. Sparse and mostly single tainted
			// blocks (depth stays tiny), so it never rivals the density of the real front just behind it.
			double excess = dist - localRadius;
			double reach = 400.0 + 60.0 * stage;
			if (excess > reach) return;
			double reachChance = Math.max(0.0, 1.0 - excess / reach) * 0.35;
			if (RNG.nextDouble() > reachChance) return;
			depth = 0.05;
		} else {
			return; // outside the front for now; try again if this chunk loads later
		}

		state.seededChunks.add(key);
		seedChunkSurface(level, cp, depth, stage);
		seedChunkCaves(level, cp, depth, stage);
	}

	private static void seedChunkSurface(ServerLevel level, ChunkPos cp, double depth, int stage) {
		int spots = 2 + (int) Math.round(depth * 5) + Math.min(3, stage / 3);
		for (int i = 0; i < spots; i++) {
			int x = cp.getMinBlockX() + RNG.nextInt(16);
			int z = cp.getMinBlockZ() + RNG.nextInt(16);
			int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
			BlockPos p = new BlockPos(x, top, z);
			BlockState st = level.getBlockState(p);
			if (!InfectionUtil.isInfectible(level, p, st)) continue;
			placeSeed(level, p, depth, stage);
			if (stage >= 7 && RNG.nextFloat() < 0.55F) seedBiomePatch(level, p, depth, stage);
		}
	}

	/** Late-stage territory grows in distinct patches: mold meadows, stalk thickets and crystal-scarred ground. */
	private static void seedBiomePatch(ServerLevel level, BlockPos center, double depth, int stage) {
		int radius = 2 + Math.min(2, stage - 7);
		for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
			if (dx * dx + dz * dz > radius * radius || RNG.nextFloat() > 0.72F) continue;
			BlockPos p = center.offset(dx, 0, dz);
			if (!loaded(level, p) || !InfectionUtil.isInfectible(level, p, level.getBlockState(p))) continue;
			Block put = stage >= 9 && RNG.nextFloat() < 0.20F ? ModBlocks.SCULK_CRYSTAL_BLOCK
					: stage >= 8 && RNG.nextFloat() < 0.32F ? Blocks.SCULK
					: ModBlocks.DECAYED_BLOCK;
			InfectionUtil.infect(level, p, put.defaultBlockState());
			addNode(level, p);
			if (put == Blocks.SCULK || put == ModBlocks.SCULK_CRYSTAL_BLOCK) decorateTop(level, p, stage);
		}
	}

	/** Sculk creeps into caves independently of the surface above it, so the plague also spreads downward - aggressively. */
	private static void seedChunkCaves(ServerLevel level, ChunkPos cp, double depth, int stage) {
		int spots = 4 + (int) Math.round(depth * 9) + Math.min(11, stage);
		int minY = level.getMinY();
		int maxY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				cp.getMinBlockX() + 8, cp.getMinBlockZ() + 8) - 6;
		if (maxY <= minY) return;
		for (int i = 0; i < spots; i++) {
			// try a few different heights per attempt: most random (x,y,z) picks land inside solid stone, not a cave
			for (int tryY = 0; tryY < 4; tryY++) {
				int x = cp.getMinBlockX() + RNG.nextInt(16);
				int z = cp.getMinBlockZ() + RNG.nextInt(16);
				int y = minY + RNG.nextInt(Math.max(1, maxY - minY));
				BlockPos air = new BlockPos(x, y, z);
				if (!level.getBlockState(air).isAir() || level.canSeeSky(air)) continue;
				BlockPos floor = air.below();
				BlockState fs = level.getBlockState(floor);
				if (!InfectionUtil.isInfectible(level, floor, fs)) continue;
				placeSeed(level, floor, depth, stage);
				if (RNG.nextFloat() < 0.5F) placeVein(level, air, Direction.DOWN);
				// a struck cave pocket is worth spreading into further, not just one lonely block - and once the
				// plague is established this reaches down toward the next level of caves too, chasing new pockets
				// to grow parasite nests into much sooner than the surface front alone ever would
				for (Direction d : Direction.Plane.HORIZONTAL) {
					if (RNG.nextFloat() < 0.4F + 0.04F * stage) {
						BlockPos fn = floor.relative(d);
						if (loaded(level, fn) && InfectionUtil.isInfectible(level, fn, level.getBlockState(fn))) placeSeed(level, fn, depth, stage);
					}
				}
				if (stage >= 4 && RNG.nextFloat() < 0.30F + 0.02F * stage) {
					BlockPos below = floor.below();
					if (loaded(level, below) && InfectionUtil.isInfectible(level, below, level.getBlockState(below))) {
						placeSeed(level, below, depth, stage);
					}
				}
				break;
			}
		}
	}

	/** Turns one eligible block into tainted / decayed / full sculk, weighted by how established this territory is. */
	private static void placeSeed(ServerLevel level, BlockPos p, double depth, int stage) {
		double full = Math.min(0.55, depth * (0.25 + 0.05 * stage));
		double decayed = Math.min(0.85, full + 0.25 + 0.05 * stage);
		float r = RNG.nextFloat();
		Block put = r < full ? Blocks.SCULK : r < decayed ? ModBlocks.DECAYED_BLOCK : ModBlocks.TAINTED_BLOCK;
		InfectionUtil.infect(level, p, put.defaultBlockState());
		addNode(level, p);
		if (put == Blocks.SCULK) decorateTop(level, p, stage);
	}

	private static BlockPos findSurface(ServerLevel level, int x, int cy, int z) {
		for (int dy = 3; dy >= -5; dy--) {
			BlockPos p = new BlockPos(x, cy + dy, z);
			if (!loaded(level, p)) return null;
			BlockState st = level.getBlockState(p);
			if (InfectionUtil.isInfectible(level, p, st) && level.getBlockState(p.above()).isAir()) return p;
		}
		return null;
	}

	/** Spawns sculk blobs around a point; used by kills, carriers, parasite hatching and the core. */
	public static void infectAround(ServerLevel level, BlockPos center, int attempts, int radius, int stage) {
		if (level.dimension() == Level.NETHER) return;
		for (int i = 0; i < attempts; i++) {
			int x = center.getX() + RNG.nextInt(radius * 2 + 1) - radius;
			int z = center.getZ() + RNG.nextInt(radius * 2 + 1) - radius;
			BlockPos p = findSurface(level, x, center.getY(), z);
			if (p == null) continue;
			InfectionUtil.infect(level, p, Blocks.SCULK.defaultBlockState());
			addNode(level, p);
			decorateTop(level, p, stage);
			for (Direction d : Direction.values()) {
				BlockPos n = p.relative(d);
				if (!loaded(level, n)) continue;
				BlockState ns = level.getBlockState(n);
				if (ns.isAir() && RNG.nextFloat() < 0.3F) {
					placeVein(level, n, d.getOpposite());
				} else if (InfectionUtil.isInfectible(level, n, ns) && RNG.nextFloat() < 0.25F) {
					InfectionUtil.infect(level, n, ModBlocks.TAINTED_BLOCK.defaultBlockState());
					addNode(level, n);
				}
			}
		}
	}

	// ------------------------------------------------------------------ deaths

	private static boolean isHolyKiller(DamageSource src) {
		if (src.getDirectEntity() instanceof HolyProjectile) return true;
		return src.getEntity() instanceof Player p && p.getMainHandItem().is(ModItems.HALLOW_SWORD);
	}

	public static void onDeath(LivingEntity e, DamageSource src) {
		if (state == null || !state.active || state.cleansed || holyKill) return;
		if (!(e.level() instanceof ServerLevel level)) return;
		if (level.dimension() == Level.NETHER) return;
		if (!(e instanceof Mob) && !(e instanceof ServerPlayer)) return;
		if (e instanceof ServerPlayer sp) {
			PlayerEffects.onPlayerDeath(level, sp);
			return;
		}
		if (isHolyKiller(src)) return;
		int stage = state.stage();
		BlockPos pos = e.blockPosition();
		infectAround(level, pos, Stage.killAttempts(stage), Stage.killRadius(stage), stage);
		state.addInfection(Stage.killGain(stage));
		if (stage >= 2 && RNG.nextDouble() < (stage >= 5 ? 0.012 : 0.004)) {
			spawn(level, EntityType.WARDEN, pos);
		}
		if (stage >= 6 && RNG.nextDouble() < 0.15) {
			spawn(level, ModEntities.PARASITE, pos);
		}
	}

	// ------------------------------------------------------------------ mobs & spawning

	public static <T extends Mob> T spawn(ServerLevel level, EntityType<T> type, BlockPos pos) {
		T mob = type.create(level, EntitySpawnReason.TRIGGERED);
		if (mob == null) return null;
		mob.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		if (type == EntityType.WARDEN) mob.setPersistenceRequired();
		level.addFreshEntity(mob);
		return mob;
	}

	private static BlockPos randomNodeNear(ServerLevel level, BlockPos origin, int minD, int maxD) {
		LongArrayList list = state.nodes.get(dimKey(level));
		if (list == null || list.isEmpty()) return null;
		for (int i = 0; i < 40; i++) {
			BlockPos p = BlockPos.of(list.getLong(RNG.nextInt(list.size())));
			double d = p.distSqr(origin);
			if (d >= (double) minD * minD && d <= (double) maxD * maxD && loaded(level, p)) return p;
		}
		return null;
	}

	private static boolean airAbove(ServerLevel level, BlockPos p) {
		return level.getBlockState(p.above()).isAir() && level.getBlockState(p.above(2)).isAir();
	}

	private static <T extends Mob> int count(ServerLevel level, Class<T> cls, BlockPos c, double r) {
		return level.getEntitiesOfClass(cls, new AABB(c).inflate(r)).size();
	}

	/** Infected mobs glow, seed sculk while walking, and healthy mobs standing on sculk catch the plague. */
	private static void mobScan(int stage) {
		for (ServerLevel level : server.getAllLevels()) {
			for (ServerPlayer p : level.players()) {
				AABB box = p.getBoundingBox().inflate(48.0);
				for (Mob mob : level.getEntitiesOfClass(Mob.class, box)) {
					if (InfectedMobs.isInfected(mob)) {
						level.sendParticles(ParticleTypes.SCULK_SOUL, mob.getX(), mob.getY() + mob.getBbHeight() * 0.6, mob.getZ(), 2, 0.2, 0.3, 0.2, 0.01);
						if (stage >= 5 && RNG.nextFloat() < 0.06F) {
							infectAround(level, mob.blockPosition(), 2, 2, stage);
						}
					} else if (stage >= 5 && InfectedMobs.canInfect(mob)) {
						boolean onSculk = InfectionUtil.isInfectedFamily(level.getBlockState(mob.blockPosition().below()))
								|| InfectionUtil.isInfectedFamily(level.getBlockState(mob.blockPosition()));
						if (onSculk && RNG.nextDouble() < Stage.mobInfectChance(stage)) {
							InfectedMobs.infect(level, mob);
						}
					}
				}
			}
		}
	}

	private static void naturalSpawns(int stage) {
		if (stage < 5) return;
		for (ServerLevel level : server.getAllLevels()) {
			for (ServerPlayer p : level.players()) {
				if (p.isCreative() || p.isSpectator()) continue;
				BlockPos pp = p.blockPosition();

				if (RNG.nextDouble() < 0.35) {
					BlockPos n = randomNodeNear(level, pp, 30, 70);
					int cap = Math.min(6, stage - 3);
					if (n != null && level.canSeeSky(n.above()) && airAbove(level, n) && count(level, net.minecraft.world.entity.monster.warden.Warden.class, pp, 96) < cap) {
						spawn(level, EntityType.WARDEN, n.above());
					}
				}
				if (stage >= 6 && RNG.nextDouble() < 0.6) {
					BlockPos n = randomNodeNear(level, pp, 12, 40);
					if (n != null && airAbove(level, n) && count(level, SculkParasite.class, pp, 48) < 18) {
						spawn(level, ModEntities.PARASITE, n.above());
					}
				}
				if (stage >= 6 && RNG.nextDouble() < 0.45) {
					BlockPos n = randomNodeNear(level, pp, 16, 40);
					if (n != null && airAbove(level, n) && count(level, SculkWalker.class, pp, 48) < 16) {
						spawn(level, ModEntities.WALKER, n.above());
					}
				}
				if (stage >= 7 && RNG.nextDouble() < 0.25) {
					BlockPos n = randomNodeNear(level, pp, 20, 44);
					if (n != null && airAbove(level, n) && count(level, SculkBrute.class, pp, 64) < 6) {
						spawn(level, ModEntities.BRUTE, n.above());
					}
				}
				if (stage >= 5 && RNG.nextDouble() < 0.18
						&& count(level, dev.sculkslayer.entity.SculkCreeper.class, pp, 56) < Math.min(5, stage - 3)) {
					BlockPos n = randomNodeNear(level, pp, 18, 48);
					if (n != null && airAbove(level, n)) spawn(level, ModEntities.SCULK_CREEPER, n.above());
				}
				if (stage >= 5) hatchNests(level, pp);
				// crystal caves start subtly at stage 3 and get denser, bigger and more frequent every stage after;
				// nests can now appear from stage 5, so blooms are pushed noticeably harder from that point on
				if (stage >= 3) {
					int calls = 2 + stage / 2;
					double chance = Math.min(0.97, 0.35 + 0.11 * stage);
					for (int i = 0; i < calls; i++) {
						if (RNG.nextDouble() < chance) caveBloom(level, pp, stage);
					}
				}
			}
		}
	}

	/** Underground: sculk caves smothered in crystals, with parasite nests (and their Sculk Hoard) from stage 5. */
	private static void caveBloom(ServerLevel level, BlockPos player, int stage) {
		int radiusXZ = 24 + Math.min(28, stage * 3);
		int radiusY = 16 + Math.min(14, stage);
		int tries = 10 + stage * 2;
		int maxNests = stage >= 8 ? 3 : stage >= 7 ? 2 : 1; // dense hives can hide more than one nest once the plague is well established
		for (int t = 0; t < tries; t++) {
			BlockPos c = player.offset(RNG.nextInt(radiusXZ * 2 + 1) - radiusXZ, RNG.nextInt(radiusY * 2 + 1) - radiusY,
					RNG.nextInt(radiusXZ * 2 + 1) - radiusXZ);
			if (!loaded(level, c) || !level.getBlockState(c).isAir() || level.canSeeSky(c)) continue;
			int nestsPlaced = 0;
			float crystalChance = Math.min(0.9F, 0.40F + 0.07F * stage);
			float nestChance = Math.min(0.18F, 0.07F + 0.014F * stage);
			int spots = 50 + stage * 9;
			for (int i = 0; i < spots; i++) {
				BlockPos q = c.offset(RNG.nextInt(15) - 7, RNG.nextInt(11) - 5, RNG.nextInt(15) - 7);
				if (!loaded(level, q)) continue;
				BlockState qs = level.getBlockState(q);
				if (!InfectionUtil.isInfectible(level, q, qs)) continue;
				boolean exposed = false;
				for (Direction d : Direction.values()) {
					BlockPos n = q.relative(d);
					if (!loaded(level, n) || !level.getBlockState(n).isAir()) continue;
					exposed = true;
					if (RNG.nextFloat() < crystalChance) placeCrystal(level, n, d);
				}
				if (!exposed) continue;
				Block put = RNG.nextFloat() < Math.min(0.4F, 0.14F + 0.025F * stage) ? ModBlocks.SCULK_CRYSTAL_BLOCK
						: RNG.nextBoolean() ? Blocks.SCULK : ModBlocks.DECAYED_BLOCK;
				if (nestsPlaced < maxNests && stage >= 5 && !level.canSeeSky(q) && RNG.nextFloat() < nestChance && level.getBlockState(q.above()).isAir()
						&& state.nestsOf(dimKey(level)).size() < 400) {
					put = ModBlocks.SCULK_NEST;
					nestsPlaced++;
					state.nestsOf(dimKey(level)).add(q.asLong());
					placeNestChest(level, q.above());
				}
				InfectionUtil.infect(level, q, put.defaultBlockState());
				addNode(level, q);
				if (stage >= 3 && put != ModBlocks.SCULK_NEST) decorateFoliage(level, q, stage);
			}
			return;
		}
	}

	/** A distinct, named hoard block with its own texture and curated loot table (junk, useful salvage, rare relics). */
	private static void placeNestChest(ServerLevel level, BlockPos pos) {
		level.setBlock(pos, ModBlocks.SCULK_HOARD.defaultBlockState(), Block.UPDATE_CLIENTS);
		if (level.getBlockEntity(pos) instanceof SculkHoardBlockEntity hoard) {
			ResourceKey<LootTable> table = ResourceKey.create(Registries.LOOT_TABLE, SculkSlayer.id("chests/sculk_hoard"));
			hoard.setLootTable(table, RNG.nextLong());
		}
	}

	private static void hatchNests(ServerLevel level, BlockPos player) {
		LongArrayList nests = state.nests.get(dimKey(level));
		if (nests == null) return;
		for (int i = nests.size() - 1; i >= 0; i--) {
			BlockPos n = BlockPos.of(nests.getLong(i));
			if (n.distSqr(player) > 40 * 40 || !loaded(level, n)) continue;
			if (!level.getBlockState(n).is(ModBlocks.SCULK_NEST)) {
				nests.removeLong(i);
				continue;
			}
			if (RNG.nextFloat() < 0.55F && count(level, SculkParasite.class, n, 20) < 10) {
				for (Direction d : Direction.Plane.HORIZONTAL) {
					BlockPos s = n.relative(d);
					if (loaded(level, s) && level.getBlockState(s).isAir()) {
						spawn(level, ModEntities.PARASITE, s);
						break;
					}
				}
			}
		}
	}

	// ------------------------------------------------------------------ core

	private static void trySpawnCore() {
		ServerLevel ow = overworld();
		if (ow == null) return;
		for (ServerPlayer p : ow.players()) {
			BlockPos n = randomNodeNear(ow, p.blockPosition(), 30, 90);
			if (n == null || !airAbove(ow, n)) continue;
			BlockPos pos = n.above();
			ow.setBlock(pos, ModBlocks.SCULK_CORE.defaultBlockState(), Block.UPDATE_CLIENTS);
			for (Direction d : Direction.values()) {
				BlockPos c = pos.relative(d);
				if (loaded(ow, c) && ow.getBlockState(c).isAir()) placeCrystal(ow, c, d);
			}
			state.core = pos;
			state.coreWeak = false;
			broadcast("THE SCULK CORE HAS AWAKENED at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
					+ ". Weaken it with a Holy Grenade, then mine it with the Hallow Pickaxe for 3 minutes.");
			return;
		}
	}

	private static void coreTick(int stage) {
		ServerLevel ow = overworld();
		if (ow == null || state.core == null) return;
		BlockPos core = state.core;
		if (!loaded(ow, core)) return;
		if (!ow.getBlockState(core).is(ModBlocks.SCULK_CORE)) {
			finishWorld();
			return;
		}
		if (!state.coreWeak) return;
		ow.sendParticles(ParticleTypes.SCULK_SOUL, core.getX() + 0.5, core.getY() + 1.2, core.getZ() + 0.5, 12, 0.6, 0.6, 0.6, 0.05);
		infectAround(ow, core, 10, 10, stage);
		if (state.ticks % 60 == 0) {
			if (count(ow, SculkWalker.class, core, 40) < 8) spawn(ow, ModEntities.WALKER, core.offset(RNG.nextInt(9) - 4, 1, RNG.nextInt(9) - 4));
			if (count(ow, SculkParasite.class, core, 40) < 12) spawn(ow, ModEntities.PARASITE, core.offset(RNG.nextInt(9) - 4, 1, RNG.nextInt(9) - 4));
			if (count(ow, SculkBrute.class, core, 48) < 3 && RNG.nextFloat() < 0.4F) spawn(ow, ModEntities.BRUTE, core.offset(RNG.nextInt(9) - 4, 1, RNG.nextInt(9) - 4));
		}
		if (state.ticks % 400 == 0 && count(ow, net.minecraft.world.entity.monster.warden.Warden.class, core, 60) < 3) {
			spawn(ow, EntityType.WARDEN, core.offset(RNG.nextInt(11) - 5, 1, RNG.nextInt(11) - 5));
		}
	}

	public static void onGrenade(ServerLevel level, BlockPos pos) {
		if (state == null || state.core == null || state.coreWeak) return;
		if (level.dimension() != Level.OVERWORLD || pos.distSqr(state.core) > 100) return;
		state.coreWeak = true;
		broadcast("The Sculk Core is WEAKENED and enraged! Mine it with the Hallow Pickaxe (3 minutes) before the world is lost.");
		level.playSound(null, state.core, SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 4.0F, 0.6F);
	}

	public static void finishWorld() {
		if (state == null || state.cleansed) return;
		state.cleansed = true;
		state.core = null;
		state.coreWeak = false;
		state.infection = 0;
		state.parasitized.clear();
		state.nodes.clear();
		state.nests.clear();
		broadcast("THE SCULK CORE IS DESTROYED. The plague dies. The world is cleansed!");
		syncAll();
		save();
	}

	/** After victory the cleansing wave keeps sweeping around every player. */
	private static void cleansingTick() {
		if (state.ticks % 100 == 0) {
			for (ServerLevel level : server.getAllLevels()) {
				for (ServerPlayer p : level.players()) {
					HolyEffects.queuePurge(level, p.blockPosition(), 32);
					HolyEffects.smite(level, p.blockPosition(), 64, true, 0.0F);
				}
			}
		}
		if (state.ticks % 20 == 0) syncAll();
	}

	// ------------------------------------------------------------------ messaging & sync

	public static void broadcast(String text) {
		if (server != null) server.getPlayerList().broadcastSystemMessage(Component.literal("[Sculk Slayer] " + text), false);
	}

	private static void announceStage(int ns) {
		if (ns > state.lastStage) {
			broadcast("The infection has reached stage " + Stage.label(ns) + ".");
		}
	}

	public static String syncString() {
		SculkState s = state;
		BlockPos m = s.monolith == null ? BlockPos.ZERO : s.monolith;
		String core = s.core == null ? "" : s.core.getX() + "," + s.core.getY() + "," + s.core.getZ();
		return (s.active ? 1 : 0) + "|" + String.format(java.util.Locale.ROOT, "%.1f", s.infection) + "|" + Stage.label(s.stage())
				+ "|" + m.getX() + "," + m.getY() + "," + m.getZ() + "|" + core + "|" + (s.cleansed ? 1 : 0);
	}

	public static void syncAll() {
		if (server == null || state == null) return;
		String data = syncString();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			dev.sculkslayer.net.SyncPayload.send(p, data);
		}
	}

	public static void syncTo(ServerPlayer p) {
		if (state != null) dev.sculkslayer.net.SyncPayload.send(p, syncString());
	}

	private SculkManager() {}
}
