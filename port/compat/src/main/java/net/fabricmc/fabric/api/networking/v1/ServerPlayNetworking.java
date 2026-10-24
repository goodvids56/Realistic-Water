package net.fabricmc.fabric.api.networking.v1;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

public final class ServerPlayNetworking {
	private static final Map<Identifier, PlayPayloadHandler<?>> RECEIVERS = new ConcurrentHashMap<>();

	private ServerPlayNetworking() {
	}

	public static <T extends CustomPacketPayload> boolean registerGlobalReceiver(CustomPacketPayload.Type<T> type, PlayPayloadHandler<T> handler) {
		return RECEIVERS.putIfAbsent(type.id(), handler) == null;
	}

	public static @Nullable PlayPayloadHandler<?> unregisterGlobalReceiver(Identifier id) {
		return RECEIVERS.remove(id);
	}

	/// Called by the loader's network handler when a serverbound payload arrives.
	@SuppressWarnings("unchecked")
	public static <T extends CustomPacketPayload> void receive(T payload, MinecraftServer server, ServerPlayer player) {
		PlayPayloadHandler<T> handler = (PlayPayloadHandler<T>) RECEIVERS.get(payload.type().id());
		if (handler != null) {
			handler.receive(payload, new ContextImpl(server, player));
		}
	}

	@FunctionalInterface
	public interface PlayPayloadHandler<T extends CustomPacketPayload> {
		void receive(T payload, Context context);
	}

	public interface Context {
		MinecraftServer server();

		ServerPlayer player();
	}

	private record ContextImpl(MinecraftServer server, ServerPlayer player) implements Context {
	}
}
