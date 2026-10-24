package net.fabricmc.fabric.api.client.networking.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.jspecify.annotations.Nullable;

public final class ClientPlayConnectionEvents {
	public static final Event<Disconnect> DISCONNECT = EventFactory.createArrayBacked(Disconnect.class, listeners -> (handler, client) -> {
		for (Disconnect listener : listeners) {
			listener.onPlayDisconnect(handler, client);
		}
	});

	private ClientPlayConnectionEvents() {
	}

	@FunctionalInterface
	public interface Disconnect {
		// The handler can be null on loaders that don't expose it when the connection closes.
		void onPlayDisconnect(@Nullable ClientPacketListener handler, Minecraft client);
	}
}
