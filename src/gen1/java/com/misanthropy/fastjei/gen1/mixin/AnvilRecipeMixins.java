package com.misanthropy.fastjei.gen1.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.misanthropy.fastjei.FastJeiConfig;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.library.plugins.vanilla.anvil.AnvilRecipeMaker;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

import java.util.stream.Stream;

public final class AnvilRecipeMixins {

	private AnvilRecipeMixins() {}

	@Mixin(value = AnvilRecipeMaker.class, remap = false)
	public static abstract class SkipGeneratedEnchantments {

		@WrapMethod(method = "getBookEnchantmentRecipes("
				+ "Lmezz/jei/api/recipe/vanilla/IVanillaRecipeFactory;"
				+ "Lmezz/jei/api/runtime/IIngredientManager;"
				+ "Lmezz/jei/api/ingredients/IIngredientHelper;"
				+ ")Ljava/util/stream/Stream;")
		private static Stream<IJeiAnvilRecipe> fastjei$skipBookEnchantments(
				IVanillaRecipeFactory vanillaRecipeFactory,
				IIngredientManager ingredientManager,
				IIngredientHelper<ItemStack> ingredientHelper,
				Operation<Stream<IJeiAnvilRecipe>> original
		) {
			if (FastJeiConfig.SKIP_GENERATED_ENCHANTMENT_RECIPES) {
				return Stream.empty();
			}
			return original.call(vanillaRecipeFactory, ingredientManager, ingredientHelper);
		}
	}

	@Mixin(value = AnvilRecipeMaker.class, remap = false)
	public static abstract class SkipGeneratedRepairs {

		@WrapMethod(method = "getRepairRecipes("
				+ "Lmezz/jei/api/recipe/vanilla/IVanillaRecipeFactory;"
				+ "Lmezz/jei/api/ingredients/IIngredientHelper;"
				+ ")Ljava/util/stream/Stream;")
		private static Stream<IJeiAnvilRecipe> fastjei$skipRepairs(
				IVanillaRecipeFactory vanillaRecipeFactory,
				IIngredientHelper<ItemStack> ingredientHelper,
				Operation<Stream<IJeiAnvilRecipe>> original
		) {
			if (FastJeiConfig.SKIP_GENERATED_REPAIR_RECIPES) {
				return Stream.empty();
			}
			return original.call(vanillaRecipeFactory, ingredientHelper);
		}
	}
}
