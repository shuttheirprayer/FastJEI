package com.misanthropy.fastjei.gen3.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.misanthropy.fastjei.BrewingRecipeIndex;
import mezz.jei.api.recipe.vanilla.IJeiBrewingRecipe;
import mezz.jei.library.util.BrewingRecipeMakerCommon;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Mixin(value = BrewingRecipeMakerCommon.class, remap = false)
public abstract class BrewingRecipeMakerCommonMixin {

	@Unique
	private static Set<ItemStack> fastjei$brewedPotions;

	@Inject(method = "getVanillaBrewingRecipes", at = @At("HEAD"), require = 0)
	private static void fastjei$startBrewing(CallbackInfoReturnable<?> cir) {
		fastjei$brewedPotions = Collections.newSetFromMap(new IdentityHashMap<>());
	}

	@Inject(method = "getVanillaBrewingRecipes", at = @At("RETURN"), require = 0)
	private static void fastjei$finishBrewing(CallbackInfoReturnable<?> cir) {
		fastjei$brewedPotions = null;
	}

	@WrapOperation(
			method = "getNewPotions",
			at = @At(value = "INVOKE", target = "Ljava/util/Collection;iterator()Ljava/util/Iterator;", ordinal = 0),
			require = 0
	)
	private static Iterator<ItemStack> fastjei$onlyUnbrewedPotions(
			Collection<ItemStack> knownPotions,
			Operation<Iterator<ItemStack>> original
	) {
		Set<ItemStack> brewed = fastjei$brewedPotions;
		if (brewed == null) {
			return original.call(knownPotions);
		}
		List<ItemStack> unbrewed = new ArrayList<>();
		Iterator<ItemStack> iterator = original.call(knownPotions);
		while (iterator.hasNext()) {
			ItemStack potion = iterator.next();
			if (brewed.add(potion)) {
				unbrewed.add(potion);
			}
		}
		return unbrewed.iterator();
	}

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
