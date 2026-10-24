package net.fabricmc.fabric.api.client.networking.v1;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import me.skaffy.port.compat.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class ClientPlayNetworking {
	private static final Map<Identifier, PlayPayloadHandler<?>> RECEIVERS = new ConcurrentHashMap<>();

	private ClientPlayNetworking() {
	}

	public static <T extends CustomPacketPayload> boolean registerGlobalReceiver(CustomPacketPayload.Type<T> type, PlayPayloadHandler<T> handler) {
		return RECEIVERS.putIfAbsent(type.id(), handler) == null;
	}

	public static @Nullable PlayPayloadHandler<?> unregisterGlobalReceiver(Identifier id) {
		return RECEIVERS.remove(id);
	}

	public static boolean canSend(CustomPacketPayload.Type<?> type) {
		return Platform.get().canSendToServer(type);
	}

	public static void send(CustomPacketPayload payload) {
		if (Minecraft.getInstance().getConnection() == null) {
			throw new IllegalStateException("Cannot send packets when not in game!");
		}
		Platform.get().sendToServer(payload);
	}

	/// Called by the loader's network handler when a clientbound payload arrives.
	@SuppressWarnings("unchecked")
	public static <T extends CustomPacketPayload> void receive(T payload, Minecraft client, LocalPlayer player) {
		PlayPayloadHandler<T> handler = (PlayPayloadHandler<T>) RECEIVERS.get(payload.type().id());
		if (handler != null) {
			handler.receive(payload, new ContextImpl(client, player));
		}
	}

	@FunctionalInterface
	public interface PlayPayloadHandler<T extends CustomPacketPayload> {
		void receive(T payload, Context context);
	}

	public interface Context {
		Minecraft client();

		LocalPlayer player();
	}

	private record ContextImpl(Minecraft client, LocalPlayer player) implements Context {
	}
}
