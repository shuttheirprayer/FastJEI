package com.misanthropy.fastjei.mixin;

import com.misanthropy.fastjei.JeiGeneration;

import java.util.Map;

public final class Gen3MixinPlugin extends GenerationMixinPlugin {

	private static final String PACKAGE = "com.misanthropy.fastjei.gen3.mixin.";

	@Override
	protected JeiGeneration generation() {
		return JeiGeneration.GEN3;
	}

	@Override
	protected Map<String, RequiredMethod> requiredMethods() {
		return Map.of(
				PACKAGE + "AnvilRecipeMixins$SkipGeneratedEnchantments",
				new RequiredMethod("generated anvil enchanting skip", "getBookEnchantmentRecipes", "()Ljava/util/stream/Stream;"),
				PACKAGE + "AnvilRecipeMixins$QuietAnvilFill",
				RequiredMethod.of("quiet anvil menu fill", "setAnvilMenu"),
				PACKAGE + "GrindstoneRecipeMixins$SkipGeneratedDisenchanting",
				RequiredMethod.of("generated grindstone disenchanting skip", "getDisenchantRecipes"),
				PACKAGE + "GrindstoneRecipeMixins$QuietGrindstoneFill",
				RequiredMethod.of("quiet grindstone menu fill", "getGrindstoneResult"),
				PACKAGE + "AnvilRecipeMixins$SkipGeneratedRepairs",
				new RequiredMethod("generated anvil repair skip", "getRepairRecipes", "()Ljava/util/stream/Stream;"),
				PACKAGE + "GrindstoneRecipeMixins$SkipGeneratedRepairs",
				RequiredMethod.of("generated grindstone repair skip", "getRepairRecipes"),
				PACKAGE + "BrewingRecipeMakerCommonMixin",
				RequiredMethod.of("indexed brewing lookup", "getNewPotions"),
				PACKAGE + "VanillaPluginTimingMixin",
				RequiredMethod.of("recipe build timings", "registerRecipes")
		);
	}

	@Override
	protected Map<String, String> configGates() {
		return Map.of(PACKAGE + "BrewingRecipeMakerCommonMixin", "indexedBrewingLookup");
	}
}
