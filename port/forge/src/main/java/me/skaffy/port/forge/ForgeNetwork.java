package me.skaffy.port.forge;

import me.skaffy.port.compat.Payloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.payload.PayloadFlow;
import net.minecraftforge.network.payload.PayloadProtocol;
import org.jspecify.annotations.Nullable;

/// All of upstream's play payloads on one Forge channel.
final class ForgeNetwork {
	private static @Nullable Channel<CustomPacketPayload> channel;

	private ForgeNetwork() {
	}

	static @Nullable Channel<CustomPacketPayload> channel() {
		return channel;
	}

	static void init() {
		if (Payloads.SERVERBOUND.isEmpty() && Payloads.CLIENTBOUND.isEmpty()) {
			return;
		}
		// Optional so players with the mod can still join servers without it (and vice versa), like on Fabric.
		PayloadProtocol<RegistryFriendlyByteBuf, CustomPacketPayload> play = ChannelBuilder
			.named(Identifier.fromNamespaceAndPath(RealisticWaterForge.MOD_ID, "play"))
			.optional()
			.payloadChannel()
			.play();
		PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> flow = null;
		if (!Payloads.SERVERBOUND.isEmpty()) {
			flow = play.serverbound();
			for (Payloads.Registration<?> registration : Payloads.SERVERBOUND) {
				flow = addServerbound(flow, registration);
			}
		}
		if (!Payloads.CLIENTBOUND.isEmpty()) {
			flow = play.clientbound();
			for (Payloads.Registration<?> registration : Payloads.CLIENTBOUND) {
				flow = addClientbound(flow, registration);
			}
		}
		channel = flow.build();
	}

	// Fabric receivers run on the network thread; upstream hops to the main thread itself.
	private static <T extends CustomPacketPayload> PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> addServerbound(
		PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> flow, Payloads.Registration<T> registration
	) {
		return flow.add(registration.type(), codec(registration), (payload, context) -> {
			ServerPlayer player = context.getSender();
			if (player != null) {
				ServerPlayNetworking.receive(payload, player.level().getServer(), player);
			}
			context.setPacketHandled(true);
		});
	}

	private static <T extends CustomPacketPayload> PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> addClientbound(
		PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> flow, Payloads.Registration<T> registration
	) {
		return flow.add(registration.type(), codec(registration), (payload, context) -> {
			ForgeClient.receive(payload);
			context.setPacketHandled(true);
		});
	}

	// A codec that reads any supertype of the buffer can read the buffer itself.
	@SuppressWarnings("unchecked")
	private static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> codec(Payloads.Registration<T> registration) {
		return (StreamCodec<RegistryFriendlyByteBuf, T>) registration.codec();
	}
}
