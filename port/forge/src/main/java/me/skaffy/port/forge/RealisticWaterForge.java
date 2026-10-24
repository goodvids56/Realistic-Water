package me.skaffy.port.forge;

import java.util.ArrayList;
import java.util.List;
import me.skaffy.port.compat.Entrypoints;
import me.skaffy.port.compat.Platform;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryManager;

@Mod(RealisticWaterForge.MOD_ID)
public final class RealisticWaterForge {
	public static final String MOD_ID = "realistic_water";

	private boolean initialized;

	public RealisticWaterForge(FMLJavaModLoadingContext context) {
		BusGroup modBus = context.getModBusGroup();
		boolean client = FMLEnvironment.dist.isClient();
		Platform.set(new ForgePlatform());
		// Upstream registers straight into the built-in registries in one go, while Forge only opens each
		// registry during its own RegisterEvent. Fabric runs the client entrypoint right after the main one.
		RegisterEvent.getBus(modBus).addListener(Priority.HIGHEST, event -> {
			if (!this.initialized) {
				this.initialized = true;
				withRegistriesOpen(event, () -> {
					Entrypoints.runMain();
					if (client) {
						Entrypoints.runClient();
					}
				});
				ForgeFluidTypes.assign(Entrypoints.modId());
			}
		});
		// Payloads are only known once upstream's initializer has run.
		FMLCommonSetupEvent.getBus(modBus).addListener(event -> event.enqueueWork(ForgeNetwork::init));
		BuildCreativeModeTabContentsEvent.BUS.addListener(event ->
			CreativeModeTabEvents.fire(event.getTabKey(), event.getTab(), ForgeTabOutput.create(event)));
		if (client) {
			ForgeClient.init();
		}
	}

	// Forge would unfreeze each of these for its own RegisterEvent later and freezes it again afterwards,
	// so leaving them frozen is the state it expects. The registry whose event is running stays open.
	private static void withRegistriesOpen(RegisterEvent current, Runnable action) {
		List<ForgeRegistry<?>> opened = new ArrayList<>();
		for (Identifier key : BuiltInRegistries.REGISTRY.keySet()) {
			ForgeRegistry<?> registry = RegistryManager.ACTIVE.getRegistry(key);
			if (registry != null) {
				registry.unfreeze();
				opened.add(registry);
			}
		}
		try {
			action.run();
		} finally {
			for (ForgeRegistry<?> registry : opened) {
				if (!registry.getRegistryKey().equals(current.getRegistryKey())) {
					registry.freeze();
				}
			}
		}
	}
}
