package dev.sculkslayer.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import dev.sculkslayer.registry.ModBlockEntities;

/**
 * A named, uniquely-textured loot container for parasite nests. Reuses the vanilla container/loot-table
 * machinery (the same {@code setLootTable} used by {@link net.minecraft.world.level.block.entity.ChestBlockEntity})
 * via {@link RandomizableContainerBlockEntity}, and the vanilla 3-row chest menu, so no custom GUI is needed.
 */
public class SculkHoardBlockEntity extends RandomizableContainerBlockEntity {
	private NonNullList<ItemStack> items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);

	public SculkHoardBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SCULK_HOARD, pos, state);
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> list) {
		this.items = list;
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("container.sculkslayer.sculk_hoard");
	}

	@Override
	protected AbstractContainerMenu createMenu(int syncId, Inventory playerInventory) {
		return ChestMenu.threeRows(syncId, playerInventory, this);
	}

	@Override
	public int getContainerSize() {
		return 27;
	}
}
