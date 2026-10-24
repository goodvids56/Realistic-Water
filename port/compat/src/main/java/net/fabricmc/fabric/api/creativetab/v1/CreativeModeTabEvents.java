package net.fabricmc.fabric.api.creativetab.v1;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

public final class CreativeModeTabEvents {
	private static final Map<ResourceKey<CreativeModeTab>, Event<ModifyOutput>> EVENTS = new ConcurrentHashMap<>();

	public static final Event<ModifyOutputAll> MODIFY_OUTPUT_ALL = EventFactory.createArrayBacked(ModifyOutputAll.class, listeners -> (tab, output) -> {
		for (ModifyOutputAll listener : listeners) {
			listener.modifyOutput(tab, output);
		}
	});

	private CreativeModeTabEvents() {
	}

	public static Event<ModifyOutput> modifyOutputEvent(ResourceKey<CreativeModeTab> tab) {
		return EVENTS.computeIfAbsent(tab, key -> EventFactory.createArrayBacked(ModifyOutput.class, listeners -> output -> {
			for (ModifyOutput listener : listeners) {
				listener.modifyOutput(output);
			}
		}));
	}

	/// Called by the loader while a tab's contents are being built.
	public static void fire(ResourceKey<CreativeModeTab> key, CreativeModeTab tab, FabricCreativeModeTabOutput output) {
		Event<ModifyOutput> event = EVENTS.get(key);
		if (event != null) {
			event.invoker().modifyOutput(output);
		}
		MODIFY_OUTPUT_ALL.invoker().modifyOutput(tab, output);
	}

	@FunctionalInterface
	public interface ModifyOutput {
		void modifyOutput(FabricCreativeModeTabOutput output);
	}

	@FunctionalInterface
	public interface ModifyOutputAll {
		void modifyOutput(CreativeModeTab tab, FabricCreativeModeTabOutput output);
	}
}
