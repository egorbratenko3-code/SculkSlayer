package dev.sculkslayer.registry;

import java.util.Map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import dev.sculkslayer.SculkSlayer;

public final class ModArmorMaterials {
	public static final int BASE_DURABILITY = 37;

	public static final ResourceKey<EquipmentAsset> HALLOW_KEY =
			ResourceKey.create(EquipmentAssets.ROOT_ID, SculkSlayer.id("hallow"));

	public static final ArmorMaterial HALLOW = new ArmorMaterial(
			BASE_DURABILITY,
			Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 8, ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 3),
			15,
			SoundEvents.ARMOR_EQUIP_NETHERITE,
			3.0F,
			0.1F,
			ModTags.HALLOW_REPAIR,
			HALLOW_KEY);

	/** Gold-speed tool that can still harvest everything (needed for ancient debris). */
	public static final ToolMaterial HALLOW_TOOL = new ToolMaterial(
			BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2031, 12.0F, 0.0F, 22, ModTags.HALLOW_REPAIR);

	private ModArmorMaterials() {}
}
