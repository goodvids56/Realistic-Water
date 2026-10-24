package me.skaffy.port.neoforge;

import java.nio.file.Path;
import me.skaffy.port.compat.Platform;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;

final class NeoForgePlatform implements Platform {
	@Override
	public Path configDir() {
		return FMLPaths.CONFIGDIR.get();
	}

	@Override
	public Path gameDir() {
		return FMLPaths.GAMEDIR.get();
	}

	@Override
	public boolean isModLoaded(String id) {
		return ModList.get().isLoaded(id);
	}

	@Override
	public boolean isDevelopment() {
		return !FMLEnvironment.isProduction();
	}

	@Override
	public boolean canSendToServer(CustomPacketPayload.Type<?> type) {
		return NeoForgeClient.canSend(type);
	}

	@Override
	public void sendToServer(CustomPacketPayload payload) {
		NeoForgeClient.send(payload);
	}
}
