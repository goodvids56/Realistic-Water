package me.skaffy.port.neoforge;

import me.skaffy.port.compat.Entrypoints;
import me.skaffy.port.compat.Payloads;
import me.skaffy.port.compat.Platform;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(RealisticWaterNeoForge.MOD_ID)
public final class RealisticWaterNeoForge {
	public static final String MOD_ID = "realistic_water";

	private boolean initialized;

	public RealisticWaterNeoForge(IEventBus modBus, Dist dist) {
		Platform.set(new NeoForgePlatform());
		// Upstream registers straight into the built-in registries, which are only open during RegisterEvent.
		// Fabric runs the client entrypoint right after the main one, so do the same.
		modBus.addListener(EventPriority.HIGHEST, RegisterEvent.class, event -> {
			if (!this.initialized) {
				this.initialized = true;
				Entrypoints.runMain();
				NeoForgeFluidTypes.assign(Entrypoints.modId());
				if (dist.isClient()) {
					Entrypoints.runClient();
				}
			}
		});
		modBus.addListener(RegisterPayloadHandlersEvent.class, RealisticWaterNeoForge::registerPayloads);
		modBus.addListener(BuildCreativeModeTabContentsEvent.class, event ->
			CreativeModeTabEvents.fire(event.getTabKey(), event.getTab(), NeoForgeTabOutput.create(event)));
		if (dist.isClient()) {
			NeoForgeClient.init(modBus);
		}
	}

	private static void registerPayloads(RegisterPayloadHandlersEvent event) {
		// Optional so players with the mod can still join servers without it (and vice versa), like on Fabric.
		// Fabric receivers run on the network thread; upstream hops to the main thread itself.
		PayloadRegistrar registrar = event.registrar("1").optional().executesOn(HandlerThread.NETWORK);
		for (Payloads.Registration<?> registration : Payloads.SERVERBOUND) {
			registerServerbound(registrar, registration);
		}
		for (Payloads.Registration<?> registration : Payloads.CLIENTBOUND) {
			registerClientbound(registrar, registration);
		}
	}

	private static <T extends CustomPacketPayload> void registerServerbound(PayloadRegistrar registrar, Payloads.Registration<T> registration) {
		registrar.playToServer(registration.type(), registration.codec(), (payload, context) -> {
			if (context.player() instanceof ServerPlayer player) {
				ServerPlayNetworking.receive(payload, player.level().getServer(), player);
			}
		});
	}

	private static <T extends CustomPacketPayload> void registerClientbound(PayloadRegistrar registrar, Payloads.Registration<T> registration) {
		registrar.playToClient(registration.type(), registration.codec(), (T payload, IPayloadContext context) -> NeoForgeClient.receive(payload, context));
	}
}
