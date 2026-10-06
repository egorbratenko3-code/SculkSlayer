package dev.sculkslayer.registry;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntityType;

import dev.sculkslayer.SculkSlayer;
import dev.sculkslayer.block.SculkHoardBlockEntity;

public final class ModBlockEntities {
	public static final BlockEntityType<SculkHoardBlockEntity> SCULK_HOARD = register(
			"sculk_hoard", SculkHoardBlockEntity::new, ModBlocks.SCULK_HOARD);

	private static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> register(
			String name, FabricBlockEntityTypeBuilder.Factory<? extends T> factory, net.minecraft.world.level.block.Block... blocks) {
		ResourceKey<BlockEntityType<?>> key = ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, SculkSlayer.id(name));
		return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, key, FabricBlockEntityTypeBuilder.<T>create(factory, blocks).build());
	}

	public static void init() {}

	private ModBlockEntities() {}
}
