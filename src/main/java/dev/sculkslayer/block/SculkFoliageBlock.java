package dev.sculkslayer.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Small, purely decorative sculk vegetation (grass tufts, mushrooms, pebbles, debris, stalks...). Only ever placed
 * by the infection's own generation code, never by a player, so it deliberately has no {@code canSurvive} /
 * neighbour-update logic: one less block-update check to run for every one of these that exists in the world,
 * which matters once the plague is old and there are tens of thousands of them scattered around.
 */
public class SculkFoliageBlock extends Block {
	private final VoxelShape shape;

	public SculkFoliageBlock(Properties properties, float radius, float height) {
		super(properties);
		float x0 = 8.0F - radius, x1 = 8.0F + radius, z0 = 8.0F - radius, z1 = 8.0F + radius;
		this.shape = Block.box(x0, 0.0, z0, x1, height, z1);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}
}
