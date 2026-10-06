package dev.sculkslayer.registry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import dev.sculkslayer.SculkSlayer;
import dev.sculkslayer.entity.HolyProjectile;
import dev.sculkslayer.entity.SculkBrute;
import dev.sculkslayer.entity.SculkCreeper;
import dev.sculkslayer.entity.SculkParasite;
import dev.sculkslayer.entity.SculkWalker;

public final class ModEntities {
	public static final EntityType<SculkParasite> PARASITE = register("sculk_parasite",
			EntityType.Builder.<SculkParasite>of(SculkParasite::new, MobCategory.MONSTER)
					.sized(0.4F, 0.3F).clientTrackingRange(8));
	public static final EntityType<SculkWalker> WALKER = register("sculk_walker",
			EntityType.Builder.<SculkWalker>of(SculkWalker::new, MobCategory.MONSTER)
					.sized(0.6F, 1.95F).clientTrackingRange(8));
	public static final EntityType<SculkBrute> BRUTE = register("sculk_brute",
			EntityType.Builder.<SculkBrute>of(SculkBrute::new, MobCategory.MONSTER)
					.sized(0.6F, 1.95F).clientTrackingRange(10));
	public static final EntityType<SculkCreeper> SCULK_CREEPER = register("sculk_creeper",
			EntityType.Builder.<SculkCreeper>of(SculkCreeper::new, MobCategory.MONSTER)
					.sized(0.6F, 1.7F).clientTrackingRange(8));
	public static final EntityType<HolyProjectile> HOLY_PROJECTILE = register("holy_projectile",
			EntityType.Builder.<HolyProjectile>of(HolyProjectile::new, MobCategory.MISC)
					.sized(0.25F, 0.25F).clientTrackingRange(4));

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, SculkSlayer.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(PARASITE, SculkParasite.createParasiteAttributes());
		FabricDefaultAttributeRegistry.register(WALKER, SculkWalker.createWalkerAttributes());
		FabricDefaultAttributeRegistry.register(BRUTE, SculkBrute.createBruteAttributes());
		FabricDefaultAttributeRegistry.register(SCULK_CREEPER, SculkCreeper.createSculkCreeperAttributes());
	}

	private ModEntities() {}
}
