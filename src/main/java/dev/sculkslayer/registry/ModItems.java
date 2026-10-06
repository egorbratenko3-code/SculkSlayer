package dev.sculkslayer.registry;

import java.util.function.Function;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.equipment.ArmorType;

import dev.sculkslayer.SculkSlayer;
import dev.sculkslayer.item.CrucifixItem;
import dev.sculkslayer.item.HallowPickaxeItem;
import dev.sculkslayer.item.HallowAxeItem;
import dev.sculkslayer.item.HolyThrowItem;

public final class ModItems {
	public static final Item HALLOW_BAR = register("hallow_bar", Item::new, new Item.Properties());
	public static final Item SACRAMENT = register("sacrament", Item::new, new Item.Properties().rarity(Rarity.EPIC));
	public static final Item CRUCIFIX = register("crucifix", CrucifixItem::new,
			new Item.Properties().stacksTo(16).rarity(Rarity.EPIC));
	public static final Item HOLY_WATER = register("holy_water", p -> new HolyThrowItem(p, false),
			new Item.Properties().stacksTo(16).rarity(Rarity.RARE));
	public static final Item HOLY_GRENADE = register("holy_grenade", p -> new HolyThrowItem(p, true),
			new Item.Properties().stacksTo(8).rarity(Rarity.EPIC));

	public static final Item HALLOW_SWORD = register("hallow_sword", Item::new,
			new Item.Properties().sword(ModArmorMaterials.HALLOW_TOOL, 3.0F, -2.4F)
					.component(DataComponents.UNBREAKABLE, Unit.INSTANCE).rarity(Rarity.EPIC));
	public static final Item HALLOW_PICKAXE = register("hallow_pickaxe", HallowPickaxeItem::new,
			new Item.Properties().pickaxe(ModArmorMaterials.HALLOW_TOOL, 0.0F, -2.8F)
					.component(DataComponents.UNBREAKABLE, Unit.INSTANCE).rarity(Rarity.EPIC));
	public static final Item HALLOW_AXE = register("hallow_axe", HallowAxeItem::new,
			new Item.Properties().axe(ModArmorMaterials.HALLOW_TOOL, 5.0F, -3.0F)
					.component(DataComponents.UNBREAKABLE, Unit.INSTANCE).rarity(Rarity.EPIC));

	public static final Item HALLOW_HELMET = register("hallow_helmet", Item::new,
			new Item.Properties().humanoidArmor(ModArmorMaterials.HALLOW, ArmorType.HELMET)
					.durability(ArmorType.HELMET.getDurability(ModArmorMaterials.BASE_DURABILITY)).rarity(Rarity.EPIC));
	public static final Item HALLOW_BREASTPLATE = register("hallow_breastplate", Item::new,
			new Item.Properties().humanoidArmor(ModArmorMaterials.HALLOW, ArmorType.CHESTPLATE)
					.durability(ArmorType.CHESTPLATE.getDurability(ModArmorMaterials.BASE_DURABILITY)).rarity(Rarity.EPIC));
	public static final Item HALLOW_GREAVES = register("hallow_greaves", Item::new,
			new Item.Properties().humanoidArmor(ModArmorMaterials.HALLOW, ArmorType.LEGGINGS)
					.durability(ArmorType.LEGGINGS.getDurability(ModArmorMaterials.BASE_DURABILITY)).rarity(Rarity.EPIC));
	public static final Item HALLOW_BOOTS = register("hallow_boots", Item::new,
			new Item.Properties().humanoidArmor(ModArmorMaterials.HALLOW, ArmorType.BOOTS)
					.durability(ArmorType.BOOTS.getDurability(ModArmorMaterials.BASE_DURABILITY)).rarity(Rarity.EPIC));

	/** Blocking behaviour is data driven (blocks_attacks component), so we borrow it from the vanilla shield. */
	public static final Item HALLOW_SHIELD = register("hallow_shield", Item::new,
			new Item.Properties().durability(1600).repairable(ModTags.HALLOW_REPAIR).rarity(Rarity.EPIC)
					.component(DataComponents.BLOCKS_ATTACKS, Items.SHIELD.components().get(DataComponents.BLOCKS_ATTACKS))
					.component(DataComponents.EQUIPPABLE, Items.SHIELD.components().get(DataComponents.EQUIPPABLE)));

	public static final Item SCULK_PARASITE_SPAWN_EGG = register("sculk_parasite_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.PARASITE));
	public static final Item SCULK_WALKER_SPAWN_EGG = register("sculk_walker_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.WALKER));
	public static final Item SCULK_BRUTE_SPAWN_EGG = register("sculk_brute_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.BRUTE));
	public static final Item SCULK_CREEPER_SPAWN_EGG = register("sculk_creeper_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.SCULK_CREEPER));

	public static final ResourceKey<CreativeModeTab> TAB_KEY =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, SculkSlayer.id("main"));
	public static final CreativeModeTab TAB = FabricItemGroup.builder()
			.icon(() -> new ItemStack(HALLOW_SWORD))
			.title(Component.translatable("itemGroup.sculkslayer"))
			.displayItems((params, out) -> {
				out.accept(HALLOW_SWORD);
				out.accept(HALLOW_PICKAXE);
				out.accept(HALLOW_AXE);
				out.accept(HALLOW_HELMET);
				out.accept(HALLOW_BREASTPLATE);
				out.accept(HALLOW_GREAVES);
				out.accept(HALLOW_BOOTS);
				out.accept(HALLOW_SHIELD);
				out.accept(HALLOW_BAR);
				out.accept(ModBlocks.HALLOW_BLOCK);
				out.accept(ModBlocks.HALLOW_WOOD);
				out.accept(ModBlocks.HALLOW_ALTAR);
				out.accept(HOLY_WATER);
				out.accept(CRUCIFIX);
				out.accept(HOLY_GRENADE);
				out.accept(SACRAMENT);
				out.accept(ModBlocks.SCULK_CRYSTAL);
				out.accept(ModBlocks.SCULK_CRYSTAL_BLOCK);
				out.accept(ModBlocks.TAINTED_BLOCK);
				out.accept(ModBlocks.DECAYED_BLOCK);
				out.accept(ModBlocks.SCULK_NEST);
				out.accept(ModBlocks.SCULK_CORE);
				out.accept(ModBlocks.SCULK_GRASS);
				out.accept(ModBlocks.SCULK_LEAVES);
				out.accept(ModBlocks.SCULK_ROOTS);
				out.accept(ModBlocks.SCULK_VINES);
				out.accept(ModBlocks.SCULK_TENDRIL);
				out.accept(ModBlocks.SCULK_STALK);
				out.accept(ModBlocks.SCULK_PEBBLES);
				out.accept(ModBlocks.SCULK_DEBRIS);
				out.accept(ModBlocks.SCULK_MUSHROOM_SMALL);
				out.accept(ModBlocks.SCULK_MUSHROOM_MEDIUM);
				out.accept(ModBlocks.SCULK_MUSHROOM_LARGE);
				out.accept(SCULK_PARASITE_SPAWN_EGG);
				out.accept(SCULK_WALKER_SPAWN_EGG);
				out.accept(SCULK_BRUTE_SPAWN_EGG);
				out.accept(SCULK_CREEPER_SPAWN_EGG);
			})
			.build();

	private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties props) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SculkSlayer.id(name));
		T item = factory.apply(props.setId(key));
		Registry.register(BuiltInRegistries.ITEM, key, item);
		return item;
	}

	public static void init() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, TAB);
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(group -> {
			group.accept(SCULK_PARASITE_SPAWN_EGG);
			group.accept(SCULK_WALKER_SPAWN_EGG);
			group.accept(SCULK_BRUTE_SPAWN_EGG);
			group.accept(SCULK_CREEPER_SPAWN_EGG);
		});
	}

	private ModItems() {}
}
