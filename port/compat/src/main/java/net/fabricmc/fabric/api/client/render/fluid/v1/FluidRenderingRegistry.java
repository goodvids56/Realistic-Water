package net.fabricmc.fabric.api.client.render.fluid.v1;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public final class FluidRenderingRegistry {
	private static final Map<Fluid, Registration> REGISTRATIONS = new ConcurrentHashMap<>();
	private static final Map<Block, Boolean> TRANSPARENT = new ConcurrentHashMap<>();
	private static final FluidRenderHandler VANILLA = new FluidRenderHandler() {
	};

	private FluidRenderingRegistry() {
	}

	public static @Nullable FluidRenderHandler get(Fluid fluid) {
		Registration registration = REGISTRATIONS.get(fluid);
		return registration != null ? registration.handler() : null;
	}

	public static @Nullable FluidRenderHandler getOverride(Fluid fluid) {
		return get(fluid);
	}

	public static void register(Fluid fluid, FluidModel.Unbaked model, FluidRenderHandler handler) {
		REGISTRATIONS.put(fluid, new Registration(model, handler));
	}

	public static void register(Fluid fluid, FluidModel.Unbaked model) {
		register(fluid, model, VANILLA);
	}

	public static void register(Fluid still, Fluid flowing, FluidModel.Unbaked model, FluidRenderHandler handler) {
		register(still, model, handler);
		register(flowing, model, handler);
	}

	public static void register(Fluid still, Fluid flowing, FluidModel.Unbaked model) {
		register(still, flowing, model, VANILLA);
	}

	public static void setBlockTransparency(Block block, boolean transparent) {
		TRANSPARENT.put(block, transparent);
	}

	public static boolean isBlockTransparent(Block block) {
		return TRANSPARENT.getOrDefault(block, false);
	}

	/// For the loader: everything registered so far.
	public static Map<Fluid, Registration> registrations() {
		return Map.copyOf(REGISTRATIONS);
	}

	/// Whether a handler replaces vanilla rendering (the default handler just calls vanilla).
	public static boolean isCustom(FluidRenderHandler handler) {
		return handler != VANILLA;
	}

	public record Registration(FluidModel.Unbaked model, FluidRenderHandler handler) {
	}
}
