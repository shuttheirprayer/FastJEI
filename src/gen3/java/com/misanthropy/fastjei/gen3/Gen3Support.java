package com.misanthropy.fastjei.gen3;

import com.misanthropy.fastjei.Fastjei;
import com.misanthropy.fastjei.JeiCompat;
import com.misanthropy.fastjei.gen3.mixin.SafeIngredientUtilAccessor;

import java.util.concurrent.ConcurrentHashMap;

public final class Gen3Support implements JeiCompat {

	@Override
	public String[] mixinTargets() {
		return new String[]{
				"mezz.jei.gui.search.ElementSearch",
				"mezz.jei.common.util.SafeIngredientUtil",
				"mezz.jei.library.plugins.vanilla.anvil.AnvilHelper",
				"mezz.jei.library.plugins.vanilla.VanillaPlugin",
				"mezz.jei.forge.platform.RecipeHelper",
				"mezz.jei.library.plugins.vanilla.anvil.AnvilRecipeMaker",
				"mezz.jei.library.plugins.vanilla.grindstone.GrindstoneRecipeMaker",
				"mezz.jei.common.ingredients.itemStacks.TypedItemStack"
		};
	}

	@Override
	public void installConcurrentSafetySets() {
		try {
			SafeIngredientUtilAccessor.fastjei$setCrashingBatchRenderers(ConcurrentHashMap.newKeySet());
			SafeIngredientUtilAccessor.fastjei$setCrashingRenderers(ConcurrentHashMap.newKeySet());
			SafeIngredientUtilAccessor.fastjei$setCrashingTooltips(ConcurrentHashMap.newKeySet());
			Fastjei.concurrentSafetySetsInstalled = true;
		} catch (Throwable t) {
			Fastjei.LOGGER.error("[FastJEI] Could not make JEI's ingredient crash trackers thread-safe - " +
					"the parallel tooltip scan will stay disabled for safety.", t);
		}
	}
}
