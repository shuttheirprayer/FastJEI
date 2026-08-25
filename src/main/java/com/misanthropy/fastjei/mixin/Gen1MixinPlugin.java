package com.misanthropy.fastjei.mixin;

import com.misanthropy.fastjei.JeiGeneration;

import java.util.Map;

public final class Gen1MixinPlugin extends GenerationMixinPlugin {

	private static final String PACKAGE = "com.misanthropy.fastjei.gen1.mixin.";

	@Override
	protected JeiGeneration generation() {
		return JeiGeneration.GEN1;
	}

	@Override
	protected Map<String, RequiredMethod> requiredMethods() {
		return Map.of(
				PACKAGE + "AnvilRecipeMixins$SkipGeneratedEnchantments",
				new RequiredMethod("generated anvil enchanting skip", "getBookEnchantmentRecipes",
						"(Lmezz/jei/api/recipe/vanilla/IVanillaRecipeFactory;"
								+ "Lmezz/jei/api/runtime/IIngredientManager;"
								+ "Lmezz/jei/api/ingredients/IIngredientHelper;"
								+ ")Ljava/util/stream/Stream;"),
				PACKAGE + "AnvilRecipeMixins$SkipGeneratedRepairs",
				new RequiredMethod("generated anvil repair skip", "getRepairRecipes",
						"(Lmezz/jei/api/recipe/vanilla/IVanillaRecipeFactory;"
								+ "Lmezz/jei/api/ingredients/IIngredientHelper;"
								+ ")Ljava/util/stream/Stream;"),
				PACKAGE + "VanillaPluginTimingMixin",
				RequiredMethod.of("recipe build timings", "registerRecipes")
		);
	}
}
