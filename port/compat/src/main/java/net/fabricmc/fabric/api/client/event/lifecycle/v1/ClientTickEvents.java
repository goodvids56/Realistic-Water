package net.fabricmc.fabric.api.client.event.lifecycle.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public final class ClientTickEvents {
	public static final Event<StartTick> START_CLIENT_TICK = EventFactory.createArrayBacked(StartTick.class, listeners -> client -> {
		for (StartTick listener : listeners) {
			listener.onStartTick(client);
		}
	});

	public static final Event<EndTick> END_CLIENT_TICK = EventFactory.createArrayBacked(EndTick.class, listeners -> client -> {
		for (EndTick listener : listeners) {
			listener.onEndTick(client);
		}
	});

	public static final Event<StartLevelTick> START_LEVEL_TICK = EventFactory.createArrayBacked(StartLevelTick.class, listeners -> level -> {
		for (StartLevelTick listener : listeners) {
			listener.onStartTick(level);
		}
	});

	public static final Event<EndLevelTick> END_LEVEL_TICK = EventFactory.createArrayBacked(EndLevelTick.class, listeners -> level -> {
		for (EndLevelTick listener : listeners) {
			listener.onEndTick(level);
		}
	});

	private ClientTickEvents() {
	}

	@FunctionalInterface
	public interface StartTick {
		void onStartTick(Minecraft client);
	}

	@FunctionalInterface
	public interface EndTick {
		void onEndTick(Minecraft client);
	}

	@FunctionalInterface
	public interface StartLevelTick {
		void onStartTick(ClientLevel level);
	}

	@FunctionalInterface
	public interface EndLevelTick {
		void onEndTick(ClientLevel level);
	}
}
