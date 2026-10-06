package dev.sculkslayer.infection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.Property;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import dev.sculkslayer.registry.ModBlocks;
import dev.sculkslayer.registry.ModEntities;
import dev.sculkslayer.registry.ModTags;

public final class InfectionUtil {
	private static Set<Block> family;
	private static final Map<String, String> ORIGINAL = new HashMap<>();

	public static void infect(ServerLevel level, BlockPos pos, BlockState infected) {
		BlockState old = level.getBlockState(pos);
		if (isInfectible(level, pos, old) || !old.getFluidState().isEmpty()) {
			ORIGINAL.put(SculkManager.dimKey(level) + ":" + pos.asLong(), encode(old));
		}
		level.setBlock(pos, infected, Block.UPDATE_CLIENTS);
	}

	public static Set<Block> family() {
		if (family == null) {
			family = new HashSet<>(List.of(
					Blocks.SCULK, Blocks.SCULK_VEIN, Blocks.SCULK_SENSOR, Blocks.SCULK_SHRIEKER, Blocks.SCULK_CATALYST,
					ModBlocks.TAINTED_BLOCK, ModBlocks.DECAYED_BLOCK, ModBlocks.SCULK_CRYSTAL,
					ModBlocks.SCULK_CRYSTAL_BLOCK, ModBlocks.SCULK_NEST,
					ModBlocks.SCULK_GRASS, ModBlocks.SCULK_LEAVES, ModBlocks.SCULK_ROOTS, ModBlocks.SCULK_VINES,
					ModBlocks.SCULK_TENDRIL, ModBlocks.SCULK_STALK, ModBlocks.SCULK_PEBBLES, ModBlocks.SCULK_DEBRIS,
					ModBlocks.SCULK_MUSHROOM_SMALL, ModBlocks.SCULK_MUSHROOM_MEDIUM, ModBlocks.SCULK_MUSHROOM_LARGE));
		}
		return family;
	}

	public static boolean isInfectedFamily(BlockState state) {
		return family().contains(state.getBlock());
	}

	/** Blocks that are removed to air (not replaced by ground) when cleansed. */
	public static boolean isDecoration(BlockState state) {
		Block b = state.getBlock();
		return b == Blocks.SCULK_VEIN || b == Blocks.SCULK_SENSOR || b == Blocks.SCULK_SHRIEKER
				|| b == Blocks.SCULK_CATALYST || b == ModBlocks.SCULK_CRYSTAL
				|| b == ModBlocks.SCULK_GRASS || b == ModBlocks.SCULK_LEAVES || b == ModBlocks.SCULK_ROOTS
				|| b == ModBlocks.SCULK_VINES || b == ModBlocks.SCULK_TENDRIL || b == ModBlocks.SCULK_STALK
				|| b == ModBlocks.SCULK_PEBBLES || b == ModBlocks.SCULK_DEBRIS
				|| b == ModBlocks.SCULK_MUSHROOM_SMALL || b == ModBlocks.SCULK_MUSHROOM_MEDIUM || b == ModBlocks.SCULK_MUSHROOM_LARGE;
	}

	public static boolean isProtected(BlockPos pos) {
		SculkState s = SculkManager.state();
		return s != null && s.inProtected(pos);
	}

	/** Solid, full-cube, un-owned blocks that the plague can eat. */
	public static boolean isInfectible(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.isAir() || !state.getFluidState().isEmpty()) return false;
		if (isInfectedFamily(state) || state.is(ModTags.SCULK_IMMUNE) || state.hasBlockEntity()) return false;
		if (isProtected(pos)) return false;
		if (state.getDestroySpeed(level, pos) < 0) return false;
		return state.isCollisionShapeFullBlock(level, pos);
	}

	/** What a cleansed block turns into. */
	public static BlockState cleanState(ServerLevel level, BlockPos pos, BlockState old) {
		String encoded = ORIGINAL.remove(SculkManager.dimKey(level) + ":" + pos.asLong());
		if (encoded == null && isDecoration(old)) return Blocks.AIR.defaultBlockState();
		BlockState original = encoded == null ? null : decode(encoded);
		return original == null ? (pos.getY() < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState()) : original;
	}

	public static Map<String, String> saveOriginals() { return new HashMap<>(ORIGINAL); }
	public static void loadOriginals(Map<String, String> saved) { ORIGINAL.clear(); if (saved != null) ORIGINAL.putAll(saved); }
	private static String encode(BlockState state) {
		StringBuilder out = new StringBuilder(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
		state.getValues().forEach((property, value) -> out.append('|').append(property.getName()).append('=').append(name(property, value)));
		return out.toString();
	}
	private static <T extends Comparable<T>> String name(Property<T> property, Comparable<?> value) {
		return property.getName((T) value);
	}
	private static BlockState decode(String encoded) {
		String[] parts = encoded.split("\\|");
		Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(parts[0]));
		BlockState result = block.defaultBlockState();
		for (int i = 1; i < parts.length; i++) {
			int split = parts[i].indexOf('=');
			if (split <= 0) continue;
			result = property(result, parts[i].substring(0, split), parts[i].substring(split + 1));
		}
		return result;
	}
	@SuppressWarnings("unchecked")
	private static <T extends Comparable<T>> BlockState property(BlockState state, String key, String value) {
		Property<T> property = (Property<T>) state.getBlock().getStateDefinition().getProperty(key);
		if (property == null) return state;
		Optional<T> parsed = property.getValue(value);
		return parsed.map(t -> state.setValue(property, t)).orElse(state);
	}

	public static boolean isSculkMobType(Entity e) {
		EntityType<?> t = e.getType();
		return t == ModEntities.PARASITE || t == ModEntities.WALKER || t == ModEntities.BRUTE
				|| t == ModEntities.SCULK_CREEPER;
	}

	private InfectionUtil() {}
}
