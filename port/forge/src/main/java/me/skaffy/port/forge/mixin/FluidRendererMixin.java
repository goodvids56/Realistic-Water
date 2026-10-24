package me.skaffy.port.forge.mixin;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// Lets a registered FluidRenderHandler replace vanilla fluid rendering, as on Fabric.
@Mixin(FluidRenderer.class)
abstract class FluidRendererMixin {
	// Set while a handler runs, so a handler that falls back to vanilla rendering doesn't loop.
	@Unique
	private static final ThreadLocal<Boolean> realisticWater$inHandler = ThreadLocal.withInitial(() -> false);

	@Inject(method = "tesselate", at = @At("HEAD"), cancellable = true)
	private void realisticWater$customHandler(
		BlockAndTintGetter level, BlockPos pos, FluidRenderer.Output output, BlockState blockState, FluidState fluidState, CallbackInfo ci
	) {
		if (realisticWater$inHandler.get()) {
			return;
		}
		FluidRenderHandler handler = FluidRenderingRegistry.get(fluidState.getType());
		if (handler == null || !FluidRenderingRegistry.isCustom(handler)) {
			return;
		}
		realisticWater$inHandler.set(true);
		try {
			handler.renderFluid((FluidRenderer) (Object) this, pos, level, output, blockState, fluidState);
		} finally {
			realisticWater$inHandler.set(false);
		}
		ci.cancel();
	}
}
