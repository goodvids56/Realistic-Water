package me.skaffy.port.compat;

import java.nio.file.Path;
import java.util.Objects;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/// What a mod loader has to provide for the Fabric API compat layer.
public interface Platform {
	Path configDir();

	Path gameDir();

	boolean isModLoaded(String id);

	boolean isDevelopment();

	boolean canSendToServer(CustomPacketPayload.Type<?> type);

	void sendToServer(CustomPacketPayload payload);

	static Platform get() {
		return Objects.requireNonNull(Holder.platform, "Fabric compat platform not initialised");
	}

	static void set(Platform platform) {
		Holder.platform = platform;
	}

	final class Holder {
		private static volatile Platform platform;

		private Holder() {
		}
	}
}
