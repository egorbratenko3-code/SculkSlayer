package dev.sculkslayer.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import dev.sculkslayer.SculkSlayer;

/** Server -> client HUD data: "active|infected%|stage|mx,my,mz|cx,cy,cz|cleansed". */
public record SyncPayload(String data) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SyncPayload> ID = new Type<>(SculkSlayer.id("sync"));
	public static final StreamCodec<FriendlyByteBuf, SyncPayload> CODEC = CustomPacketPayload.codec(SyncPayload::write, SyncPayload::new);

	public SyncPayload(FriendlyByteBuf buf) {
		this(buf.readUtf());
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(data);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static void register() {
		PayloadTypeRegistry.playS2C().register(ID, CODEC);
	}

	public static void send(ServerPlayer player, String data) {
		ServerPlayNetworking.send(player, new SyncPayload(data));
	}
}
