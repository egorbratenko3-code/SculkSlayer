package dev.sculkslayer.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.CreeperRenderer;

import dev.sculkslayer.SculkSlayer;
import dev.sculkslayer.net.SyncPayload;
import dev.sculkslayer.registry.ModBlocks;
import dev.sculkslayer.registry.ModEntities;

public class SculkSlayerClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(SyncPayload.ID, (payload, context) -> HudData.parse(payload.data()));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> HudData.reset());

		HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, SculkSlayer.id("infection_hud"), InfectionHud::render);

		BlockRenderLayerMap.putBlock(ModBlocks.SCULK_CRYSTAL, ChunkSectionLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(ModBlocks.SCULK_GRASS, ChunkSectionLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(ModBlocks.SCULK_LEAVES, ChunkSectionLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(ModBlocks.SCULK_ROOTS, ChunkSectionLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(ModBlocks.SCULK_VINES, ChunkSectionLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(ModBlocks.SCULK_TENDRIL, ChunkSectionLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(ModBlocks.SCULK_STALK, ChunkSectionLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(ModBlocks.SCULK_PEBBLES, ChunkSectionLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(ModBlocks.SCULK_DEBRIS, ChunkSectionLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(ModBlocks.SCULK_MUSHROOM_SMALL, ChunkSectionLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(ModBlocks.SCULK_MUSHROOM_MEDIUM, ChunkSectionLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(ModBlocks.SCULK_MUSHROOM_LARGE, ChunkSectionLayer.CUTOUT);

		EntityRendererRegistry.register(ModEntities.PARASITE, SculkParasiteRenderer::new);
		EntityRendererRegistry.register(ModEntities.WALKER, SculkWalkerRenderer::new);
		EntityRendererRegistry.register(ModEntities.BRUTE, SculkBruteRenderer::new);
		EntityRendererRegistry.register(ModEntities.HOLY_PROJECTILE, ThrownItemRenderer::new);
		EntityRendererRegistry.register(ModEntities.SCULK_CREEPER, CreeperRenderer::new);
	}
}
