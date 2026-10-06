package dev.sculkslayer.infection;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;

import dev.sculkslayer.SculkSlayer;

/** Persistent per-world state, stored as JSON next to level.dat. */
public class SculkState {
	// Compact (non-pretty) JSON: with tens of thousands of tracked nodes this matters a lot for save/load time.
	private static final Gson GSON = new GsonBuilder().create();
	public static final int MAX_NODES = 20000;

	public boolean active;
	public boolean cleansed;
	/** True once the monolith was auto-built (or started manually); prevents endless auto-restarts after /stop. */
	public boolean autoStarted;
	/** One-time repair marker for monolith temple details added in the later 1.2 update. */
	public boolean sanctuaryPatched;
	public double infection;
	/** Radius (blocks, XZ) of the infection front around the monolith. Grows on its own, capped per stage. */
	public double frontRadius;
	public long ticks;
	public int lastStage;
	public String dim = "minecraft:overworld";

	public BlockPos monolith, altar, signSword, signPick, signAxe, boxMin, boxMax;
	public BlockPos core;
	public boolean coreWeak;

	public final Set<UUID> gotSword = new HashSet<>();
	public final Set<UUID> gotPick = new HashSet<>();
	public final Set<UUID> gotAxe = new HashSet<>();
	public final Set<UUID> parasitized = new HashSet<>();

	/** Recently infected block positions per dimension, used as spreading frontier. */
	public final Map<String, LongArrayList> nodes = new HashMap<>();
	public final Map<String, LongArrayList> nests = new HashMap<>();
	/**
	 * Chunks (packed ChunkPos.asLong()) the front has already seeded with terrain, so we never reseed the same
	 * chunk twice in one session. Session-only: NOT persisted to disk. A world with a large, long-played front can
	 * accumulate hundreds of thousands of entries, and writing that to JSON on every save is what caused the
	 * infinite load/save hang - so after a restart, already-visited chunks may get one extra seeding pass instead.
	 */
	public final LongOpenHashSet seededChunks = new LongOpenHashSet();
	public static final int MAX_SEEDED_CHUNKS = 500000;

	public int stage() { return Stage.of(infection); }

	public void addInfection(double delta) {
		infection = Math.max(0.0, Math.min(100.0, infection + delta));
	}

	public boolean inProtected(BlockPos p) {
		return boxMin != null && boxMax != null
				&& p.getX() >= boxMin.getX() && p.getX() <= boxMax.getX()
				&& p.getY() >= boxMin.getY() && p.getY() <= boxMax.getY()
				&& p.getZ() >= boxMin.getZ() && p.getZ() <= boxMax.getZ();
	}

	public LongArrayList nodesOf(String dimension) {
		return nodes.computeIfAbsent(dimension, k -> new LongArrayList());
	}

	public LongArrayList nestsOf(String dimension) {
		return nests.computeIfAbsent(dimension, k -> new LongArrayList());
	}

	// ------------------------------------------------------------------ persistence

	private static class Data {
		boolean active, cleansed, coreWeak, autoStarted, sanctuaryPatched;
		double infection;
		double frontRadius;
		long ticks;
		int lastStage;
		String dim;
		Long monolith, altar, signSword, signPick, signAxe, boxMin, boxMax, core;
		List<String> gotSword, gotPick, gotAxe, parasitized;
		Map<String, long[]> nodes, nests;
		Map<String, String> originals;
		// seededChunks intentionally NOT persisted (see field javadoc) - would otherwise make the save file unbounded.
	}

	private static Long enc(BlockPos p) { return p == null ? null : p.asLong(); }
	private static BlockPos dec(Long l) { return l == null ? null : BlockPos.of(l); }

	public void save(Path file) {
		Data d = new Data();
		d.active = active; d.cleansed = cleansed; d.coreWeak = coreWeak; d.autoStarted = autoStarted; d.sanctuaryPatched = sanctuaryPatched;
		d.infection = infection; d.frontRadius = frontRadius; d.ticks = ticks; d.lastStage = lastStage; d.dim = dim;
		d.monolith = enc(monolith); d.altar = enc(altar); d.signSword = enc(signSword); d.signPick = enc(signPick); d.signAxe = enc(signAxe);
		d.boxMin = enc(boxMin); d.boxMax = enc(boxMax); d.core = enc(core);
		d.gotSword = strings(gotSword); d.gotPick = strings(gotPick); d.gotAxe = strings(gotAxe); d.parasitized = strings(parasitized);
		d.nodes = new HashMap<>();
		nodes.forEach((k, v) -> d.nodes.put(k, v.toLongArray()));
		d.nests = new HashMap<>();
		nests.forEach((k, v) -> d.nests.put(k, v.toLongArray()));
		d.originals = InfectionUtil.saveOriginals();
		try {
			Files.writeString(file, GSON.toJson(d), StandardCharsets.UTF_8);
		} catch (IOException e) {
			SculkSlayer.LOGGER.error("Could not save Sculk Slayer state", e);
		}
	}

	public static SculkState load(Path file) {
		InfectionUtil.loadOriginals(null);
		SculkState s = new SculkState();
		if (!Files.exists(file)) return s;
		try {
			Data d = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Data.class);
			if (d == null) return s;
			s.active = d.active; s.cleansed = d.cleansed; s.coreWeak = d.coreWeak; s.autoStarted = d.autoStarted || d.active;
			s.sanctuaryPatched = d.sanctuaryPatched;
			s.infection = d.infection; s.frontRadius = d.frontRadius; s.ticks = d.ticks; s.lastStage = d.lastStage;
			if (d.dim != null) s.dim = d.dim;
			s.monolith = dec(d.monolith); s.altar = dec(d.altar); s.signSword = dec(d.signSword);
			s.signPick = dec(d.signPick); s.signAxe = dec(d.signAxe); s.boxMin = dec(d.boxMin); s.boxMax = dec(d.boxMax); s.core = dec(d.core);
			uuids(d.gotSword, s.gotSword); uuids(d.gotPick, s.gotPick); uuids(d.gotAxe, s.gotAxe); uuids(d.parasitized, s.parasitized);
			if (d.nodes != null) d.nodes.forEach((k, v) -> s.nodes.put(k, new LongArrayList(v)));
			if (d.nests != null) d.nests.forEach((k, v) -> s.nests.put(k, new LongArrayList(v)));
			InfectionUtil.loadOriginals(d.originals);
			// seededChunks starts empty every load by design (see field javadoc)
		} catch (Exception e) {
			SculkSlayer.LOGGER.error("Could not read Sculk Slayer state, starting fresh", e);
		}
		return s;
	}

	private static List<String> strings(Set<UUID> set) {
		List<String> out = new ArrayList<>();
		for (UUID u : set) out.add(u.toString());
		return out;
	}

	private static void uuids(List<String> in, Set<UUID> out) {
		if (in == null) return;
		for (String s : in) {
			try { out.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
		}
	}
}
