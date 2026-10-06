package dev.sculkslayer.item;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import dev.sculkslayer.infection.SculkManager;
import dev.sculkslayer.SculkSlayer;
import dev.sculkslayer.registry.ModTags;

/** A costly emergency sweep: ordinary threats die, bosses only stagger. */
public class HallowAxeItem extends Item {
	public HallowAxeItem(Properties properties) { super(properties); }

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) return InteractionResult.PASS;
		var state = SculkManager.state();
		if (state == null || !state.active || state.stage() < 7) {
			player.displayClientMessage(Component.literal("The Hallow Axe awakens at infection stage 7."), true);
			return InteractionResult.FAIL;
		}
		Identifier cooldownId = SculkSlayer.id("hallow_axe");
		if (player.getCooldowns().isOnCooldown(player.getItemInHand(hand))) return InteractionResult.FAIL;
		if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
		AABB area = player.getBoundingBox().inflate(1.0, 1.0, 1.0);
		for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, area, e -> e != player)) {
			if (target.getType().is(ModTags.HALLOW_AXE_BOSSES)) {
				target.hurtServer(serverLevel, level.damageSources().playerAttack(player), 10.0F);
			} else {
				target.hurtServer(serverLevel, level.damageSources().genericKill(), 100000.0F);
			}
		}
		player.getCooldowns().addCooldown(cooldownId, 600);
		level.playSound(null, BlockPos.containing(player.position()), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8F, 1.4F);
		serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 28, 1, 1, 1, 0.12);
		return InteractionResult.SUCCESS;
	}
}
