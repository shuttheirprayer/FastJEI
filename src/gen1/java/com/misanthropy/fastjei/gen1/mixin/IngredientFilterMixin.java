package com.misanthropy.fastjei.gen1.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.misanthropy.fastjei.FastJeiConfig;
import com.misanthropy.fastjei.Fastjei;
import mezz.jei.api.helpers.IColorHelper;
import mezz.jei.api.helpers.IModIdHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IIngredientVisibility;
import mezz.jei.common.config.IClientConfig;
import mezz.jei.common.config.IClientToggleState;
import mezz.jei.common.config.IIngredientFilterConfig;
import mezz.jei.gui.filter.IFilterTextSource;
import mezz.jei.gui.ingredients.IListElement;
import mezz.jei.gui.ingredients.IListElementInfo;
import mezz.jei.gui.ingredients.IngredientFilter;
import mezz.jei.gui.search.IElementSearch;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.List;

@Mixin(value = IngredientFilter.class, remap = false)
public abstract class IngredientFilterMixin {

	@Shadow
	private IElementSearch elementSearch;

	@Shadow
	@Final
	private IIngredientManager ingredientManager;

	@Shadow
	@Final
	private IIngredientVisibility ingredientVisibility;

	@Shadow
	public abstract void invalidateCache();

	@WrapOperation(
			method = "<init>",
			at = @At(
					value = "INVOKE",
					target = "Lmezz/jei/gui/ingredients/IngredientFilter;addIngredient(Lmezz/jei/gui/ingredients/IListElementInfo;)V"
			)
	)
	private void fastjei$skipPerItemAdd(IngredientFilter instance, IListElementInfo<?> info, Operation<Void> original) {
		if (!FastJeiConfig.BATCHED_INIT) {
			original.call(instance, info);
		}
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void fastjei$flushBatch(
			IFilterTextSource filterTextSource,
			IClientConfig clientConfig,
			IIngredientFilterConfig config,
			IIngredientManager ingredientManager,
			Comparator<IListElement<?>> ingredientComparator,
			List<IListElementInfo<?>> ingredients,
			IModIdHelper modIdHelper,
			IIngredientVisibility ingredientVisibility,
			IColorHelper colorHelper,
			IClientToggleState clientToggleState,
			CallbackInfo ci
	) {
		if (!FastJeiConfig.BATCHED_INIT) {
			return;
		}

		long start = System.nanoTime();

		for (IListElementInfo<?> info : ingredients) {
			fastjei$updateHiddenState(info);
		}
		long afterHidden = System.nanoTime();

		this.elementSearch.addAll(ingredients, this.ingredientManager);
		this.invalidateCache();

		if (FastJeiConfig.TIMINGS) {
			long now = System.nanoTime();
			Fastjei.LOGGER.info(
					"[FastJEI] Ingredient filter built with {} ingredients in {} ms (visibility: {} ms, search index: {} ms)",
					ingredients.size(),
					(now - start) / 1_000_000,
					(afterHidden - start) / 1_000_000,
					(now - afterHidden) / 1_000_000
			);
		}
	}

	@Unique
	@SuppressWarnings({"rawtypes", "unchecked"})
	private void fastjei$updateHiddenState(IListElementInfo<?> info) {
		IListElement element = info.getElement();
		ITypedIngredient typedIngredient = element.getTypedIngredient();
		boolean visible = this.ingredientVisibility.isIngredientVisible(typedIngredient);
		if (element.isVisible() != visible) {
			element.setVisible(visible);
		}
	}
}
