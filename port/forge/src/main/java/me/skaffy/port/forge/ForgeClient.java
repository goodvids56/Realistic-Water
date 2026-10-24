package me.skaffy.port.forge;

import me.skaffy.port.compat.ClientCommandsBridge;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;

final class ForgeClient {
	private static boolean started;

	private ForgeClient() {
	}

	static void init() {
		TickEvent.ClientTickEvent.Pre.BUS.addListener(event -> {
			Minecraft client = Minecraft.getInstance();
			// Forge has no "client started" event; Fabric fires it right before the first tick.
			if (!started) {
				started = true;
				ClientLifecycleEvents.CLIENT_STARTED.invoker().onClientStarted(client);
			}
			ClientTickEvents.START_CLIENT_TICK.invoker().onStartTick(client);
		});
		TickEvent.ClientTickEvent.Post.BUS.addListener(event -> ClientTickEvents.END_CLIENT_TICK.invoker().onEndTick(Minecraft.getInstance()));
		TickEvent.LevelTickEvent.Pre.BUS.addListener(event -> {
			if (event.level() instanceof ClientLevel level) {
				ClientTickEvents.START_LEVEL_TICK.invoker().onStartTick(level);
			}
		});
		TickEvent.LevelTickEvent.Post.BUS.addListener(event -> {
			if (event.level() instanceof ClientLevel level) {
				ClientTickEvents.END_LEVEL_TICK.invoker().onEndTick(level);
			}
		});
		// ChunkEvent fires for the integrated server's levels too; Fabric's client events only cover the client level.
		ChunkEvent.Load.BUS.addListener(event -> {
			if (event.getLevel() instanceof ClientLevel level && event.getChunk() instanceof LevelChunk chunk) {
				ClientChunkEvents.CHUNK_LOAD.invoker().onChunkLoad(level, chunk);
			}
		});
		ChunkEvent.Unload.BUS.addListener(event -> {
			if (event.getLevel() instanceof ClientLevel level && event.getChunk() instanceof LevelChunk chunk) {
				ClientChunkEvents.CHUNK_UNLOAD.invoker().onChunkUnload(level, chunk);
			}
		});
		ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> {
			Minecraft client = Minecraft.getInstance();
			ClientPlayConnectionEvents.DISCONNECT.invoker().onPlayDisconnect(client.getConnection(), client);
		});
		GameShuttingDownEvent.BUS.addListener(event -> ClientLifecycleEvents.CLIENT_STOPPING.invoker().onClientStopping(Minecraft.getInstance()));
		RegisterClientCommandsEvent.BUS.addListener(event -> ClientCommandsBridge.register(event.getDispatcher(), event.getBuildContext()));
	}

	static boolean canSend(CustomPacketPayload.Type<?> type) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		var channel = ForgeNetwork.channel();
		return connection != null && channel != null && channel.isRemotePresent(connection.getConnection());
	}

	static void send(CustomPacketPayload payload) {
		var channel = ForgeNetwork.channel();
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (channel != null && connection != null) {
			channel.send(payload, connection.getConnection());
		}
	}

	static <T extends CustomPacketPayload> void receive(T payload) {
		Minecraft client = Minecraft.getInstance();
		if (client.player != null) {
			ClientPlayNetworking.receive(payload, client, client.player);
		}
	}
}
