package me.skaffy.port.neoforge.mixin;

import me.skaffy.port.neoforge.NeoForgeFluidTypes;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Fluid.class)
abstract class FluidMixin {
	@Inject(method = "getFluidType", at = @At("HEAD"), cancellable = true, remap = false)
	private void realisticWater$fluidType(CallbackInfoReturnable<FluidType> cir) {
		FluidType type = NeoForgeFluidTypes.of((Fluid) (Object) this);
		if (type != null) {
			cir.setReturnValue(type);
		}
	}
}
