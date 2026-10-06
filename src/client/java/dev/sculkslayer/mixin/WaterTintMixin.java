package dev.sculkslayer.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.sculkslayer.registry.ModBlocks;

/** Darkens water near infected terrain so submerged infection is visible without tinting clean water. */
@Mixin(BiomeColors.class)
public abstract class WaterTintMixin {
	@Inject(method = "getAverageWaterColor", at = @At("RETURN"), cancellable = true)
	private static void sculkslayer$darkenInfectedWater(BlockAndTintGetter level, BlockPos pos,
			CallbackInfoReturnable<Integer> cir) {
		int nearest = 99;
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					int distance = dx * dx + dy * dy + dz * dz;
					if (distance > 4 || distance >= nearest) continue;
					BlockState state = level.getBlockState(cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz));
					if (state.is(Blocks.SCULK) || state.is(ModBlocks.TAINTED_BLOCK) || state.is(ModBlocks.DECAYED_BLOCK)
							|| state.is(ModBlocks.SCULK_NEST) || state.is(ModBlocks.SCULK_CORE)
							|| state.is(ModBlocks.SCULK_CRYSTAL_BLOCK)) nearest = distance;
				}
			}
		}
		if (nearest == 99) return;
		int color = cir.getReturnValue();
		float factor = nearest <= 1 ? 0.42F : nearest <= 2 ? 0.56F : 0.70F;
		int red = (int) (((color >> 16) & 0xFF) * factor);
		int green = (int) (((color >> 8) & 0xFF) * factor);
		int blue = Math.min(255, (int) ((color & 0xFF) * (factor + 0.08F)));
		cir.setReturnValue((red << 16) | (green << 8) | blue);
	}
}
