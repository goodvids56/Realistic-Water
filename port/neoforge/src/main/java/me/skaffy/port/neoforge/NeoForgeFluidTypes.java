package me.skaffy.port.neoforge;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;
import me.skaffy.port.compat.UpstreamFluids;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import org.jspecify.annotations.Nullable;

/// Gives upstream's fluids NeoForge's water, lava or empty FluidType, matching the tags they're in.
public final class NeoForgeFluidTypes {
	private static volatile Map<Fluid, Supplier<FluidType>> types = Map.of();

	private NeoForgeFluidTypes() {
	}

	public static @Nullable FluidType of(Fluid fluid) {
		Supplier<FluidType> type = types.get(fluid);
		return type != null ? type.get() : null;
	}

	static void assign(String namespace) {
		Map<Fluid, Supplier<FluidType>> assigned = new IdentityHashMap<>();
		UpstreamFluids.classify(namespace).forEach((fluid, kind) -> assigned.put(fluid, switch (kind) {
			case WATER -> NeoForgeMod.WATER_TYPE::value;
			case LAVA -> NeoForgeMod.LAVA_TYPE::value;
			case INERT -> NeoForgeMod.EMPTY_TYPE::value;
		}));
		types = assigned;
	}
}
