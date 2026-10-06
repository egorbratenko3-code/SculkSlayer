package dev.sculkslayer.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SilverfishRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

import dev.sculkslayer.SculkSlayer;

public class SculkParasiteRenderer extends SilverfishRenderer {
	private static final Identifier TEXTURE = SculkSlayer.id("textures/entity/sculk/parasite.png");

	public SculkParasiteRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return TEXTURE;
	}
}
