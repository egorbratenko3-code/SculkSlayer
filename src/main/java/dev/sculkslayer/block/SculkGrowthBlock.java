package dev.sculkslayer.block;

import net.minecraft.world.level.block.AmethystClusterBlock;

/**
 * Directional decorative growth that sprouts off an infected block's face (roots, tendrils, hanging vines).
 * Shares its attachment/shape behaviour with {@link SculkCrystalBlock}, which is exactly what we want: a small
 * non-solid cluster that must be backed by a solid block on the opposite side of its {@code facing} direction.
 */
public class SculkGrowthBlock extends AmethystClusterBlock {
	public SculkGrowthBlock(Properties properties) {
		super(6.0F, 3.0F, properties);
	}
}
