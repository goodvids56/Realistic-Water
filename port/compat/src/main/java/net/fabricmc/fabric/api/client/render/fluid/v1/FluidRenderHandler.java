package net.fabricmc.fabric.api.client.render.fluid.v1;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public interface FluidRenderHandler {
	/// Renders the fluid; the default does what vanilla would.
	default void renderFluid(
		FluidRenderer fluidRenderer, BlockPos pos, BlockAndTintGetter level, FluidRenderer.Output output, BlockState blockState, FluidState fluidState
	) {
		fluidRenderer.tesselate(level, pos, output, blockState, fluidState);
	}
}
