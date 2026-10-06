package dev.sculkslayer.block;

import net.minecraft.world.level.block.AmethystClusterBlock;

/** Directional, waterloggable crystal that grows from infected blocks. Behaviour is inherited from amethyst clusters. */
public class SculkCrystalBlock extends AmethystClusterBlock {
	public SculkCrystalBlock(Properties properties) {
		super(7.0F, 3.0F, properties);
	}
}
