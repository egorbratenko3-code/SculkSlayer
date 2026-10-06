package dev.sculkslayer.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.EntityType;

import dev.sculkslayer.SculkSlayer;

public final class ModTags {
	/** Blocks that can never be infected (holy blocks, bedrock, portals...). */
	public static final TagKey<Block> SCULK_IMMUNE = TagKey.create(Registries.BLOCK, SculkSlayer.id("sculk_immune"));
	public static final TagKey<Item> HALLOW_REPAIR = TagKey.create(Registries.ITEM, SculkSlayer.id("hallow_repair"));
	public static final TagKey<EntityType<?>> HALLOW_AXE_BOSSES = TagKey.create(Registries.ENTITY_TYPE, SculkSlayer.id("hallow_axe_bosses"));

	private ModTags() {}
}
