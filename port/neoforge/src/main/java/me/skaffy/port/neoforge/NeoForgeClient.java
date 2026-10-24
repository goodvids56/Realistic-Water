package me.skaffy.port.neoforge;

import me.skaffy.port.compat.ClientCommandsBridge;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.lifecycle.ClientStartedEvent;
import net.neoforged.neoforge.client.event.lifecycle.ClientStoppingEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

final class NeoForgeClient {
	private NeoForgeClient() {
	}

	static void init(IEventBus modBus) {
		modBus.addListener(RegisterFluidModelsEvent.class, NeoForgeClient::registerFluidModels);

		IEventBus bus = NeoForge.EVENT_BUS;
		bus.addListener(ClientTickEvent.Pre.class, event -> ClientTickEvents.START_CLIENT_TICK.invoker().onStartTick(Minecraft.getInstance()));
		bus.addListener(ClientTickEvent.Post.class, event -> ClientTickEvents.END_CLIENT_TICK.invoker().onEndTick(Minecraft.getInstance()));
		bus.addListener(LevelTickEvent.Pre.class, event -> {
			if (event.getLevel() instanceof ClientLevel level) {
				ClientTickEvents.START_LEVEL_TICK.invoker().onStartTick(level);
			}
		});
		bus.addListener(LevelTickEvent.Post.class, event -> {
			if (event.getLevel() instanceof ClientLevel level) {
				ClientTickEvents.END_LEVEL_TICK.invoker().onEndTick(level);
			}
		});
		// ChunkEvent fires for the integrated server's levels too; Fabric's client events only cover the client level.
		bus.addListener(ChunkEvent.Load.class, event -> {
			if (event.getLevel() instanceof ClientLevel level) {
				ClientChunkEvents.CHUNK_LOAD.invoker().onChunkLoad(level, event.getChunk());
			}
		});
		bus.addListener(ChunkEvent.Unload.class, event -> {
			if (event.getLevel() instanceof ClientLevel level) {
				ClientChunkEvents.CHUNK_UNLOAD.invoker().onChunkUnload(level, event.getChunk());
			}
		});
		bus.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> {
			Minecraft client = Minecraft.getInstance();
			ClientPacketListener handler = client.getConnection();
			ClientPlayConnectionEvents.DISCONNECT.invoker().onPlayDisconnect(handler, client);
		});
		bus.addListener(ClientStartedEvent.class, event -> ClientLifecycleEvents.CLIENT_STARTED.invoker().onClientStarted(event.getClient()));
		bus.addListener(ClientStoppingEvent.class, event -> ClientLifecycleEvents.CLIENT_STOPPING.invoker().onClientStopping(event.getClient()));
		bus.addListener(RegisterClientCommandsEvent.class, event -> ClientCommandsBridge.register(event.getDispatcher(), event.getBuildContext()));
	}

	private static void registerFluidModels(RegisterFluidModelsEvent event) {
		FluidRenderingRegistry.registrations().forEach((fluid, registration) -> event.register(model(registration), fluid));
	}

	private static FluidModel.Unbaked model(FluidRenderingRegistry.Registration registration) {
		FluidModel.Unbaked model = registration.model();
		FluidRenderHandler handler = registration.handler();
		if (!FluidRenderingRegistry.isCustom(handler)) {
			return model;
		}
		return new FluidModel.Unbaked(model.stillMaterial(), model.flowingMaterial(), model.overlayMaterial(), model.fluidTintSource(),
			(fluidRenderer, fluidState, level, pos, output, blockState) -> {
				handler.renderFluid(fluidRenderer, pos, level, output, blockState, fluidState);
				return true;
			});
	}

	static boolean canSend(CustomPacketPayload.Type<?> type) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		return connection != null && connection.hasChannel(type);
	}

	static void send(CustomPacketPayload payload) {
		ClientPacketDistributor.sendToServer(payload);
	}

	static <T extends CustomPacketPayload> void receive(T payload, IPayloadContext context) {
		if (context.player() instanceof LocalPlayer player) {
			ClientPlayNetworking.receive(payload, Minecraft.getInstance(), player);
		}
	}
}
