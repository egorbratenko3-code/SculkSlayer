package dev.sculkslayer.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;

import dev.sculkslayer.SculkSlayer;

public class SculkBruteRenderer extends ZombieRenderer {
	private static final Identifier TEXTURE = SculkSlayer.id("textures/entity/sculk/brute.png");

	public SculkBruteRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public Identifier getTextureLocation(ZombieRenderState state) {
		return TEXTURE;
	}
}
