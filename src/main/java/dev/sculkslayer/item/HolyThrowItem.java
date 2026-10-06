package dev.sculkslayer.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import dev.sculkslayer.entity.HolyProjectile;
import dev.sculkslayer.registry.ModEntities;

/** Holy Water and Holy Grenade: thrown like a splash potion. */
public class HolyThrowItem extends Item {
	private final boolean grenade;

	public HolyThrowItem(Properties properties, boolean grenade) {
		super(properties);
		this.grenade = grenade;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level instanceof ServerLevel sl) {
			HolyProjectile projectile = ModEntities.HOLY_PROJECTILE.create(sl, EntitySpawnReason.TRIGGERED);
			if (projectile != null) {
				projectile.setItem(stack.copyWithCount(1));
				projectile.setOwner(player);
				projectile.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
				projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), -10.0F, grenade ? 1.0F : 0.9F, 1.0F);
				sl.addFreshEntity(projectile);
			}
			level.playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.6F, 0.5F);
			if (!player.getAbilities().instabuild) {
				stack.shrink(1);
			}
		}
		return InteractionResult.SUCCESS;
	}
}
