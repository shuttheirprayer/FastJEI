package com.misanthropy.fastjei.gen1.mixin;

import com.misanthropy.fastjei.FastJeiConfig;
import com.misanthropy.fastjei.Fastjei;
import com.misanthropy.fastjei.WorkerPool;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.core.search.ISearchStorage;
import mezz.jei.core.search.PrefixInfo;
import mezz.jei.core.search.PrefixedSearchable;
import mezz.jei.core.search.SearchMode;
import mezz.jei.gui.ingredients.IListElement;
import mezz.jei.gui.ingredients.IListElementInfo;
import mezz.jei.gui.search.ElementSearch;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;

@Mixin(value = ElementSearch.class, remap = false)
public abstract class ElementSearchMixin {

	@Unique
	private static final int FASTJEI_MIN_PARALLEL_BATCH = 128;

	@Shadow
	@Final
	private Map<PrefixInfo<IListElementInfo<?>, IListElement<?>>, PrefixedSearchable<IListElementInfo<?>, IListElement<?>>> prefixedSearchables;

	@Shadow
	@Final
	private Map<Object, IListElement<?>> allElements;

	@Inject(method = "addAll", at = @At("HEAD"), cancellable = true)
	private void fastjei$parallelAddAll(
			Collection<IListElementInfo<?>> infos,
			IIngredientManager ingredientManager,
			CallbackInfo ci
	) {
		if (!FastJeiConfig.PARALLEL_BUILD || infos.size() < FASTJEI_MIN_PARALLEL_BATCH) {
			return;
		}
		ci.cancel();

		long start = System.nanoTime();

		for (IListElementInfo<?> info : infos) {
			Object uid = fastjei$getUid(info.getTypedIngredient(), ingredientManager);
			this.allElements.put(uid, info.getElement());
		}

		boolean parallelTooltips = FastJeiConfig.PARALLEL_TOOLTIPS && Fastjei.concurrentSafetySetsInstalled;
		List<Map.Entry<PrefixedSearchable<IListElementInfo<?>, IListElement<?>>, Boolean>> pooled = new ArrayList<>();
		List<PrefixedSearchable<IListElementInfo<?>, IListElement<?>>> callingThread = new ArrayList<>();
		for (Map.Entry<PrefixInfo<IListElementInfo<?>, IListElement<?>>, PrefixedSearchable<IListElementInfo<?>, IListElement<?>>> entry
				: this.prefixedSearchables.entrySet()) {
			PrefixedSearchable<IListElementInfo<?>, IListElement<?>> searchable = entry.getValue();
			if (searchable.getMode() == SearchMode.DISABLED) {
				continue;
			}
			boolean isTooltipPrefix = entry.getKey().getPrefix() == '#';
			if (isTooltipPrefix && !parallelTooltips) {
				callingThread.add(searchable);
			} else {
				pooled.add(new AbstractMap.SimpleImmutableEntry<>(searchable, isTooltipPrefix));
			}
		}

		ForkJoinTask<?> background = WorkerPool.get().submit(() ->
				pooled.parallelStream().forEach(entry -> fastjei$fillPrefix(entry.getKey(), infos, entry.getValue()))
		);

		for (PrefixedSearchable<IListElementInfo<?>, IListElement<?>> searchable : callingThread) {
			fastjei$fillPrefix(searchable, infos, false);
		}

		try {
			background.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("[FastJEI] Interrupted while building JEI search index", e);
		} catch (ExecutionException e) {
			Fastjei.LOGGER.error("[FastJEI] Parallel search index build failed - rebuilding sequentially", e.getCause());
			for (Map.Entry<PrefixedSearchable<IListElementInfo<?>, IListElement<?>>, Boolean> entry : pooled) {
				fastjei$fillPrefix(entry.getKey(), infos, false);
			}
		}

		if (FastJeiConfig.TIMINGS) {
			Fastjei.LOGGER.info(
					"[FastJEI] Built search indexes for {} ingredients x {} prefixes in {} ms (parallel tooltips: {})",
					infos.size(),
					pooled.size() + callingThread.size(),
					(System.nanoTime() - start) / 1_000_000,
					parallelTooltips
			);
		}
	}

	@Unique
	private static void fastjei$fillPrefix(
			PrefixedSearchable<IListElementInfo<?>, IListElement<?>> searchable,
			Collection<IListElementInfo<?>> infos,
			boolean parallelTokenize
	) {
		ISearchStorage<IListElement<?>> storage = searchable.getSearchStorage();

		if (parallelTokenize) {
			List<Map.Entry<IListElement<?>, Collection<String>>> tokenized = infos.parallelStream()
					.<Map.Entry<IListElement<?>, Collection<String>>>map(info -> new AbstractMap.SimpleImmutableEntry<>(
							info.getElement(),
							fastjei$safeGetStrings(searchable, info)
					))
					.toList();
			for (Map.Entry<IListElement<?>, Collection<String>> entry : tokenized) {
				IListElement<?> element = entry.getKey();
				for (String string : entry.getValue()) {
					storage.put(string, element);
				}
			}
			return;
		}

		for (IListElementInfo<?> info : infos) {
			Collection<String> strings = fastjei$safeGetStrings(searchable, info);
			IListElement<?> element = info.getElement();
			for (String string : strings) {
				storage.put(string, element);
			}
		}
	}

	@Unique
	private static Collection<String> fastjei$safeGetStrings(
			PrefixedSearchable<IListElementInfo<?>, IListElement<?>> searchable,
			IListElementInfo<?> info
	) {
		try {
			return searchable.getStrings(info);
		} catch (Throwable t) {
			Fastjei.LOGGER.debug("[FastJEI] getStrings failed for an ingredient, skipping it for this search prefix: {}", t.toString());
			return Collections.emptyList();
		}
	}

	@Unique
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Object fastjei$getUid(ITypedIngredient<?> typedIngredient, IIngredientManager ingredientManager) {
		IIngredientHelper helper = ingredientManager.getIngredientHelper(typedIngredient.getType());
		return helper.getUniqueId(typedIngredient.getIngredient(), UidContext.Ingredient);
	}
}
