package me.skaffy.port.forge;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;
import me.skaffy.port.compat.UpstreamFluids;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidType;
import org.jspecify.annotations.Nullable;

/// Gives upstream's fluids Forge's water, lava or empty FluidType, matching the tags they're in.
public final class ForgeFluidTypes {
	private static volatile Map<Fluid, Supplier<FluidType>> types = Map.of();

	private ForgeFluidTypes() {
	}

	public static @Nullable FluidType of(Fluid fluid) {
		Supplier<FluidType> type = types.get(fluid);
		return type != null ? type.get() : null;
	}

	static void assign(String namespace) {
		Map<Fluid, Supplier<FluidType>> assigned = new IdentityHashMap<>();
		UpstreamFluids.classify(namespace).forEach((fluid, kind) -> assigned.put(fluid, switch (kind) {
			case WATER -> ForgeMod.WATER_TYPE;
			case LAVA -> ForgeMod.LAVA_TYPE;
			case INERT -> ForgeMod.EMPTY_TYPE;
		}));
		types = assigned;
	}
}
