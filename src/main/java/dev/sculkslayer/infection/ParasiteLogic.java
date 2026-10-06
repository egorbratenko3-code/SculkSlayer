package dev.sculkslayer.infection;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import dev.sculkslayer.entity.SculkParasite;

public final class ParasiteLogic {
	public static void onParasiteHit(ServerLevel level, SculkParasite parasite, LivingEntity target) {
		SculkState s = SculkManager.state();
		if (s == null || !s.active) return;
		if (target instanceof ServerPlayer player) {
			double chance = 0.25;
			if (PlayerEffects.wearsGreaves(player)) chance *= 0.7; // greaves: -30% infection chance
			if (level.random.nextDouble() < chance && s.parasitized.add(player.getUUID())) {
				player.displayClientMessage(Component.literal("Something is crawling under your skin... find Holy Water."), true);
			}
		} else if (target instanceof Mob mob && level.random.nextDouble() < 0.5) {
			InfectedMobs.infect(level, mob);
		}
	}

	private ParasiteLogic() {}
}
