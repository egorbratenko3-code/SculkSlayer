package dev.sculkslayer.block;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;

import dev.sculkslayer.registry.ModBlocks;

/** A vanilla 3x3 crafting menu that stays valid next to the Hallow Altar (the client uses the vanilla screen). */
public class AltarMenu extends CraftingMenu {
	private final ContainerLevelAccess altarAccess;

	public AltarMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(containerId, inventory, access);
		this.altarAccess = access;
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(this.altarAccess, player, ModBlocks.HALLOW_ALTAR);
	}
}
