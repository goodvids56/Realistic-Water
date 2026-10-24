package me.skaffy.port.compat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/// Play payloads registered through PayloadTypeRegistry, for the loader to hand to its network registry.
public final class Payloads {
	public static final List<Registration<?>> SERVERBOUND = new CopyOnWriteArrayList<>();
	public static final List<Registration<?>> CLIENTBOUND = new CopyOnWriteArrayList<>();

	private Payloads() {
	}

	public record Registration<T extends CustomPacketPayload>(
		CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec
	) {
	}
}
