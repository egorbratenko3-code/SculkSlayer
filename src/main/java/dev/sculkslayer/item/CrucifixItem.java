package dev.sculkslayer.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import dev.sculkslayer.infection.HolyEffects;

/** Single use: purges a huge area of sculk and destroys wardens and every unholy creature inside. */
public class CrucifixItem extends Item {
	public static final int RADIUS = 40;

	public CrucifixItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel sl) {
			ItemStack stack = player.getItemInHand(hand);
			HolyEffects.crucifix(sl, player.blockPosition(), RADIUS, player);
			if (!player.getAbilities().instabuild) {
				stack.shrink(1);
			}
		}
		return InteractionResult.SUCCESS;
	}
}
