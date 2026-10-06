package dev.sculkslayer.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Unbreakable, immovable crafting station. All holy items must be crafted here. */
public class HallowAltarBlock extends Block {
	public HallowAltarBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			player.openMenu(new SimpleMenuProvider(
					(id, inv, p) -> new AltarMenu(id, inv, ContainerLevelAccess.create(level, pos)),
					Component.translatable("container.sculkslayer.altar")));
		}
		return InteractionResult.SUCCESS;
	}
}
