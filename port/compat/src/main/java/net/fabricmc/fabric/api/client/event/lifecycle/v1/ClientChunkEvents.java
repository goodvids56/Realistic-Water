package net.fabricmc.fabric.api.client.event.lifecycle.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.LevelChunk;

public final class ClientChunkEvents {
	public static final Event<Load> CHUNK_LOAD = EventFactory.createArrayBacked(Load.class, listeners -> (level, chunk) -> {
		for (Load listener : listeners) {
			listener.onChunkLoad(level, chunk);
		}
	});

	public static final Event<Unload> CHUNK_UNLOAD = EventFactory.createArrayBacked(Unload.class, listeners -> (level, chunk) -> {
		for (Unload listener : listeners) {
			listener.onChunkUnload(level, chunk);
		}
	});

	private ClientChunkEvents() {
	}

	@FunctionalInterface
	public interface Load {
		void onChunkLoad(ClientLevel level, LevelChunk chunk);
	}

	@FunctionalInterface
	public interface Unload {
		void onChunkUnload(ClientLevel level, LevelChunk chunk);
	}
}
