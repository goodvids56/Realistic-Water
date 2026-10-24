package net.fabricmc.fabric.api.networking.v1;

import java.util.List;
import me.skaffy.port.compat.Payloads;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface PayloadTypeRegistry<B extends FriendlyByteBuf> {
	<T extends CustomPacketPayload> CustomPacketPayload.TypeAndCodec<? super B, T> register(
		CustomPacketPayload.Type<T> type, StreamCodec<? super B, T> codec
	);

	static PayloadTypeRegistry<RegistryFriendlyByteBuf> serverboundPlay() {
		return Play.SERVERBOUND;
	}

	static PayloadTypeRegistry<RegistryFriendlyByteBuf> clientboundPlay() {
		return Play.CLIENTBOUND;
	}

	final class Play implements PayloadTypeRegistry<RegistryFriendlyByteBuf> {
		private static final Play SERVERBOUND = new Play(Payloads.SERVERBOUND);
		private static final Play CLIENTBOUND = new Play(Payloads.CLIENTBOUND);

		private final List<Payloads.Registration<?>> registrations;

		private Play(List<Payloads.Registration<?>> registrations) {
			this.registrations = registrations;
		}

		@Override
		public <T extends CustomPacketPayload> CustomPacketPayload.TypeAndCodec<? super RegistryFriendlyByteBuf, T> register(
			CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec
		) {
			for (Payloads.Registration<?> existing : this.registrations) {
				if (existing.type().id().equals(type.id())) {
					throw new IllegalArgumentException("Payload type " + type.id() + " is already registered");
				}
			}
			this.registrations.add(new Payloads.Registration<>(type, codec));
			// A codec that reads any supertype of the buffer can read the buffer itself.
			@SuppressWarnings("unchecked")
			StreamCodec<RegistryFriendlyByteBuf, T> exact = (StreamCodec<RegistryFriendlyByteBuf, T>) codec;
			return new CustomPacketPayload.TypeAndCodec<>(type, exact);
		}
	}
}
