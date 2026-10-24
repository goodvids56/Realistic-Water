package net.fabricmc.loader.api;

import java.nio.file.Path;
import me.skaffy.port.compat.Platform;

public interface FabricLoader {
	static FabricLoader getInstance() {
		return Holder.INSTANCE;
	}

	Path getConfigDir();

	Path getGameDir();

	boolean isModLoaded(String id);

	boolean isDevelopmentEnvironment();

	final class Holder {
		private static final FabricLoader INSTANCE = new FabricLoader() {
			@Override
			public Path getConfigDir() {
				return Platform.get().configDir();
			}

			@Override
			public Path getGameDir() {
				return Platform.get().gameDir();
			}

			@Override
			public boolean isModLoaded(String id) {
				return Platform.get().isModLoaded(id);
			}

			@Override
			public boolean isDevelopmentEnvironment() {
				return Platform.get().isDevelopment();
			}
		};

		private Holder() {
		}
	}
}
