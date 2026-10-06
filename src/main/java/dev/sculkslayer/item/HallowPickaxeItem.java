package dev.sculkslayer.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

import dev.sculkslayer.infection.HolyEffects;
import dev.sculkslayer.infection.InfectionUtil;
import dev.sculkslayer.registry.ModBlocks;
import dev.sculkslayer.SculkSlayer;

/**
 * Never breaks, cannot hurt mobs (see SculkEvents), instantly mines Nether blocks and sculk growth,
 * mines ancient debris very quickly, and cleanses a 3x3x3 area when used on a block.
 */
public class HallowPickaxeItem extends Item {
	private static final String[] NETHER_KEYS = {
		"netherrack", "nylium", "nether_", "basalt", "blackstone", "soul_sand", "soul_soil", "glowstone",
		"shroomlight", "magma", "crimson_", "warped_", "weeping_", "twisting_", "quartz_ore", "gilded"
	};

	public HallowPickaxeItem(Properties properties) {
		super(properties);
	}

	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state) {
		if (state.is(ModBlocks.SCULK_CORE)) {
			return 1.0F; // hardness 120 => 3 minutes
		}
		if (InfectionUtil.isInfectedFamily(state)) {
			return 2000.0F;
		}
		String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
		if (path.equals("ancient_debris")) {
			return 70.0F;
		}
		for (String key : NETHER_KEYS) {
			if (path.contains(key) && !path.contains("netherite")) {
				return 3000.0F;
			}
		}
		return super.getDestroySpeed(stack, state);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getLevel() instanceof ServerLevel level && context.getPlayer() != null) {
			Player player = context.getPlayer();
			if (player.getCooldowns().isOnCooldown(context.getItemInHand())) return InteractionResult.PASS;
			BlockPos center = context.getClickedPos();
			int cleaned = HolyEffects.purgeNow(level, center, 2, false);
			if (cleaned > 0) {
				player.getCooldowns().addCooldown(SculkSlayer.id("hallow_pickaxe"), 30);
				level.playSound(null, center, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.4F);
				player.swing(context.getHand());
				return InteractionResult.SUCCESS;
			}
		}
		return InteractionResult.PASS;
	}
}
