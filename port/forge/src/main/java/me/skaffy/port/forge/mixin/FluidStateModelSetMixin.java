package me.skaffy.port.forge.mixin;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/// Forge has no way to register models for mod fluids, so add FluidRenderingRegistry's to the baked set.
@Mixin(FluidStateModelSet.class)
abstract class FluidStateModelSetMixin {
	@Inject(method = "bake", at = @At("RETURN"), cancellable = true)
	private static void realisticWater$bakeRegistered(MaterialBaker materials, CallbackInfoReturnable<Map<Fluid, FluidModel>> cir) {
		Map<Fluid, FluidRenderingRegistry.Registration> registrations = FluidRenderingRegistry.registrations();
		if (registrations.isEmpty()) {
			return;
		}
		Map<Fluid, FluidModel> models = new HashMap<>(cir.getReturnValue());
		registrations.forEach((fluid, registration) ->
			models.putIfAbsent(fluid, registration.model().bake(materials, () -> BuiltInRegistries.FLUID.getKey(fluid).toString())));
		cir.setReturnValue(Map.copyOf(models));
	}
}
