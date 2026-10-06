package dev.sculkslayer.registry;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import dev.sculkslayer.SculkSlayer;
import dev.sculkslayer.block.HallowAltarBlock;
import dev.sculkslayer.block.SculkCrystalBlock;
import dev.sculkslayer.block.SculkFoliageBlock;
import dev.sculkslayer.block.SculkGrowthBlock;
import dev.sculkslayer.block.SculkHoardBlock;

public final class ModBlocks {
	// ---- Holy blocks ----
	public static final Block HALLOW_BLOCK = register("hallow_block", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(30.0F, 1200.0F)
					.requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> 4), true);
	public static final Block HALLOW_WOOD = register("hallow_wood", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(8.0F, 120.0F)
					.sound(SoundType.WOOD), true);
	public static final Block HALLOW_ALTAR = register("hallow_altar", HallowAltarBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(-1.0F, 3600000.0F)
					.sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> 10).pushReaction(PushReaction.BLOCK), true);

	// ---- Infection stages (block -> tainted -> decayed -> vanilla sculk) ----
	public static final Block TAINTED_BLOCK = register("tainted_block", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(1.5F, 6.0F)
					.sound(SoundType.SCULK), true);
	public static final Block DECAYED_BLOCK = register("decayed_block", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(2.0F, 6.0F)
					.sound(SoundType.SCULK).lightLevel(s -> 1), true);

	// ---- Crystals ----
	public static final Block SCULK_CRYSTAL = register("sculk_crystal", SculkCrystalBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(1.5F).noOcclusion()
					.sound(SoundType.AMETHYST_CLUSTER).lightLevel(s -> 5).pushReaction(PushReaction.DESTROY), true);
	public static final Block SCULK_CRYSTAL_BLOCK = register("sculk_crystal_block", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(2.0F, 6.0F)
					.requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(s -> 7), true);

	// ---- Nests & core ----
	public static final Block SCULK_NEST = register("sculk_nest", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(3.0F, 6.0F)
					.sound(SoundType.SCULK).lightLevel(s -> 3), true);
	/** A uniquely-textured loot container found in parasite nests. See SculkHoardBlockEntity. */
	public static final Block SCULK_HOARD = register("sculk_hoard", SculkHoardBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(2.5F, 6.0F)
					.sound(SoundType.AMETHYST_CLUSTER).lightLevel(s -> 6), true);
	/** Hardness 120 + speed 1.0 with the Hallow pickaxe = exactly 3 minutes of mining. */
	public static final Block SCULK_CORE = register("sculk_core", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(120.0F, 3600000.0F)
					.sound(SoundType.SCULK_CATALYST).lightLevel(s -> 15), true);

	// ---- Decorative vegetation (1.0.6): purely cosmetic, placed only by generation, from Stage 3 onward ----
	public static final Block SCULK_GRASS = register("sculk_grass",
			p -> new SculkFoliageBlock(p, 6.5F, 8.0F),
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).noCollision().instabreak()
					.sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY), true);
	public static final Block SCULK_LEAVES = register("sculk_leaves",
			p -> new SculkFoliageBlock(p, 7.0F, 12.0F),
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).noCollision().noOcclusion().instabreak()
					.sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY), true);
	public static final Block SCULK_MUSHROOM_SMALL = register("sculk_mushroom_small",
			p -> new SculkFoliageBlock(p, 3.5F, 6.0F),
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).noCollision().instabreak()
					.sound(SoundType.FUNGUS).lightLevel(s -> 2).pushReaction(PushReaction.DESTROY), true);
	public static final Block SCULK_MUSHROOM_MEDIUM = register("sculk_mushroom_medium",
			p -> new SculkFoliageBlock(p, 4.5F, 10.0F),
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).noCollision().instabreak()
					.sound(SoundType.FUNGUS).lightLevel(s -> 3).pushReaction(PushReaction.DESTROY), true);
	public static final Block SCULK_MUSHROOM_LARGE = register("sculk_mushroom_large",
			p -> new SculkFoliageBlock(p, 6.5F, 15.0F),
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).noCollision().instabreak()
					.sound(SoundType.FUNGUS).lightLevel(s -> 4).pushReaction(PushReaction.DESTROY), true);
	public static final Block SCULK_STALK = register("sculk_stalk",
			p -> new SculkFoliageBlock(p, 3.0F, 16.0F),
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).noCollision().instabreak()
					.sound(SoundType.SCULK).pushReaction(PushReaction.DESTROY), true);
	public static final Block SCULK_PEBBLES = register("sculk_pebbles",
			p -> new SculkFoliageBlock(p, 6.0F, 3.0F),
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).noCollision().instabreak()
					.sound(SoundType.SCULK).pushReaction(PushReaction.DESTROY), true);
	public static final Block SCULK_DEBRIS = register("sculk_debris",
			p -> new SculkFoliageBlock(p, 6.5F, 3.0F),
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).noCollision().instabreak()
					.sound(SoundType.SCULK).pushReaction(PushReaction.DESTROY), true);

	// ---- Decorative growths that sprout off a block face: roots, tendrils, hanging vines ----
	public static final Block SCULK_ROOTS = register("sculk_roots", SculkGrowthBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(1.0F).noOcclusion()
					.sound(SoundType.SCULK).lightLevel(s -> 1).pushReaction(PushReaction.DESTROY), true);
	public static final Block SCULK_VINES = register("sculk_vines", SculkGrowthBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(1.0F).noOcclusion()
					.sound(SoundType.SCULK).pushReaction(PushReaction.DESTROY), true);
	public static final Block SCULK_TENDRIL = register("sculk_tendril", SculkGrowthBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(1.0F).noOcclusion()
					.sound(SoundType.SCULK).lightLevel(s -> 2).pushReaction(PushReaction.DESTROY), true);

	private static <T extends Block> T register(String name, Function<BlockBehaviour.Properties, T> factory,
			BlockBehaviour.Properties props, boolean withItem) {
		Identifier id = SculkSlayer.id(name);
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
		T block = factory.apply(props.setId(blockKey));
		Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
		if (withItem) {
			ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
			Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey)));
		}
		return block;
	}

	public static void init() {}

	private ModBlocks() {}
}
