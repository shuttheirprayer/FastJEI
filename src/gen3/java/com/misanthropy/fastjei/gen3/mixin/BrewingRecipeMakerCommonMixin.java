package com.misanthropy.fastjei.gen3.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.misanthropy.fastjei.BrewingRecipeIndex;
import mezz.jei.api.recipe.vanilla.IJeiBrewingRecipe;
import mezz.jei.library.util.BrewingRecipeMakerCommon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;
import java.util.stream.Stream;

@Mixin(value = BrewingRecipeMakerCommon.class, remap = false)
public abstract class BrewingRecipeMakerCommonMixin {

	@WrapOperation(
			method = "getNewPotions",
			at = @At(value = "INVOKE", target = "Ljava/util/Collection;stream()Ljava/util/stream/Stream;"),
			require = 0
	)
	private static Stream<IJeiBrewingRecipe> fastjei$indexedLookup(
			Collection<IJeiBrewingRecipe> recipes,
			Operation<Stream<IJeiBrewingRecipe>> original,
			@Local(ordinal = 0) IJeiBrewingRecipe recipe
	) {
		if (!BrewingRecipeIndex.usable(recipes)) {
			return original.call(recipes);
		}
		return Stream.ofNullable((IJeiBrewingRecipe) BrewingRecipeIndex.find(recipe));
	}

	@WrapOperation(
			method = "getNewPotions",
			at = @At(value = "INVOKE", target = "Ljava/util/Collection;add(Ljava/lang/Object;)Z"),
			require = 0
	)
	private static boolean fastjei$trackAdd(
			Collection<IJeiBrewingRecipe> recipes,
			Object recipe,
			Operation<Boolean> original
	) {
		BrewingRecipeIndex.added(recipe);
		return original.call(recipes, recipe);
	}

	@WrapOperation(
			method = "getNewPotions",
			at = @At(value = "INVOKE", target = "Ljava/util/Collection;remove(Ljava/lang/Object;)Z"),
			require = 0
	)
	private static boolean fastjei$trackRemove(
			Collection<IJeiBrewingRecipe> recipes,
			Object recipe,
			Operation<Boolean> original
	) {
		BrewingRecipeIndex.removed(recipe);
		return original.call(recipes, recipe);
	}
}
