package me.skaffy.port.forge.mixin;

import me.skaffy.port.forge.ForgeFluidTypes;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Fluid.class)
abstract class FluidMixin {
	@Inject(method = "getFluidType", at = @At("HEAD"), cancellable = true, remap = false)
	private void realisticWater$fluidType(CallbackInfoReturnable<FluidType> cir) {
		FluidType type = ForgeFluidTypes.of((Fluid) (Object) this);
		if (type != null) {
			cir.setReturnValue(type);
		}
	}
}
