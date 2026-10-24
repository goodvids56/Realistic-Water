package me.skaffy.port.forge;

import java.nio.file.Path;
import me.skaffy.port.compat.Platform;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

final class ForgePlatform implements Platform {
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
		return ModList.isLoaded(id);
	}

	@Override
	public boolean isDevelopment() {
		return !FMLEnvironment.production;
	}

	@Override
	public boolean canSendToServer(CustomPacketPayload.Type<?> type) {
		return ForgeClient.canSend(type);
	}

	@Override
	public void sendToServer(CustomPacketPayload payload) {
		ForgeClient.send(payload);
	}
}
