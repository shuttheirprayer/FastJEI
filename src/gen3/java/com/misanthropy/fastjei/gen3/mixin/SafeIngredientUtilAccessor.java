package com.misanthropy.fastjei.gen3.mixin;

import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.common.util.SafeIngredientUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(value = SafeIngredientUtil.class, remap = false)
public interface SafeIngredientUtilAccessor {

	@Mutable
	@Accessor("CRASHING_INGREDIENT_BATCH_RENDERERS")
	static void fastjei$setCrashingBatchRenderers(Set<IIngredientRenderer<?>> value) {
		throw new AssertionError("mixin accessor not applied");
	}

	@Mutable
	@Accessor("CRASHING_INGREDIENT_RENDERERS")
	static void fastjei$setCrashingRenderers(Set<Object> value) {
		throw new AssertionError("mixin accessor not applied");
	}

	@Mutable
	@Accessor("CRASHING_INGREDIENT_TOOLTIPS")
	static void fastjei$setCrashingTooltips(Set<Object> value) {
		throw new AssertionError("mixin accessor not applied");
	}
}
