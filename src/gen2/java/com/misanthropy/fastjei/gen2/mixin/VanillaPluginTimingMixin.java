package com.misanthropy.fastjei.gen2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.misanthropy.fastjei.FastJeiConfig;
import com.misanthropy.fastjei.Fastjei;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.library.plugins.vanilla.VanillaPlugin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = VanillaPlugin.class, remap = false)
public abstract class VanillaPluginTimingMixin {

	@Unique
	private static long fastjei$lastNanos;

	@Inject(method = "registerRecipes", at = @At("HEAD"))
	private void fastjei$beginTiming(IRecipeRegistration registration, CallbackInfo ci) {
		fastjei$lastNanos = System.nanoTime();
	}

	@WrapOperation(
			method = "registerRecipes",
			at = @At(
					value = "INVOKE",
					target = "Lmezz/jei/api/registration/IRecipeRegistration;addRecipes(Lmezz/jei/api/recipe/RecipeType;Ljava/util/List;)V"
			)
	)
	private void fastjei$timeCategory(
			IRecipeRegistration registration,
			RecipeType<?> type,
			List<?> recipes,
			Operation<Void> original
	) {
		long built = System.nanoTime() - fastjei$lastNanos;
		original.call(registration, type, recipes);
		fastjei$lastNanos = System.nanoTime();
		if (FastJeiConfig.TIMINGS) {
			Fastjei.LOGGER.info("[FastJEI] jei:minecraft {} - {} recipes built in {} ms",
					type.getUid(), recipes.size(), built / 1_000_000L);
		}
	}
}
