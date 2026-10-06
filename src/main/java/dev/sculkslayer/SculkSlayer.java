package dev.sculkslayer;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.sculkslayer.command.ModCommands;
import dev.sculkslayer.infection.SculkEvents;
import dev.sculkslayer.net.SyncPayload;
import dev.sculkslayer.registry.ModBlockEntities;
import dev.sculkslayer.registry.ModBlocks;
import dev.sculkslayer.registry.ModEntities;
import dev.sculkslayer.registry.ModItems;

public class SculkSlayer implements ModInitializer {
	public static final String MOD_ID = "sculkslayer";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		// Order matters: items reference blocks, entities reference items.
		ModBlocks.init();
		ModBlockEntities.init();
		ModItems.init();
		ModEntities.init();
		SyncPayload.register();
		ModCommands.register();
		SculkEvents.register();
		LOGGER.info("Sculk Slayer loaded. The plague is patient.");
	}
}
