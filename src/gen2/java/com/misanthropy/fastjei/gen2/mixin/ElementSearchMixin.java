package com.misanthropy.fastjei.gen2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.misanthropy.fastjei.FastJeiConfig;
import com.misanthropy.fastjei.gen2.SearchIndexBuilder;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.core.search.CombinedSearchables;
import mezz.jei.core.search.PrefixInfo;
import mezz.jei.core.search.PrefixedSearchable;
import mezz.jei.gui.ingredients.IListElement;
import mezz.jei.gui.ingredients.IListElementInfo;
import mezz.jei.gui.search.ElementPrefixParser;
import mezz.jei.gui.search.ElementSearch;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;

@Mixin(value = ElementSearch.class, remap = false)
public abstract class ElementSearchMixin {

	@Unique
	private static final int FASTJEI_MIN_PARALLEL_BATCH = 128;

	@Shadow
	@Final
	private java.util.Map<PrefixInfo<IListElementInfo<?>, IListElement<?>>, PrefixedSearchable<IListElementInfo<?>, IListElement<?>>> prefixedSearchables;

	@Shadow
	@Final
	private CombinedSearchables<IListElement<?>> combinedSearchables;

	@Unique
	private List<PrefixInfo<IListElementInfo<?>, IListElement<?>>> fastjei$deferredPrefixInfos;

	@WrapOperation(
			method = "<init>",
			at = @At(
					value = "INVOKE",
					target = "Lmezz/jei/gui/search/ElementPrefixParser;allPrefixInfos()Ljava/util/Collection;"
			)
	)
	private Collection<PrefixInfo<IListElementInfo<?>, IListElement<?>>> fastjei$deferPrefixBuild(
			ElementPrefixParser parser,
			Operation<Collection<PrefixInfo<IListElementInfo<?>, IListElement<?>>>> original
	) {
		Collection<PrefixInfo<IListElementInfo<?>, IListElement<?>>> prefixInfos = original.call(parser);
		if (!FastJeiConfig.PARALLEL_BUILD) {
			return prefixInfos;
		}
		this.fastjei$deferredPrefixInfos = List.copyOf(prefixInfos);
		return List.of();
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void fastjei$buildPrefixIndexes(
			ElementPrefixParser parser,
			Collection<IListElementInfo<?>> infos,
			IIngredientManager ingredientManager,
			CallbackInfo ci
	) {
		List<PrefixInfo<IListElementInfo<?>, IListElement<?>>> prefixInfos = this.fastjei$deferredPrefixInfos;
		if (prefixInfos == null) {
			return;
		}
		this.fastjei$deferredPrefixInfos = null;

		boolean parallel = infos.size() >= FASTJEI_MIN_PARALLEL_BATCH;
		List<PrefixedSearchable<IListElementInfo<?>, IListElement<?>>> searchables =
				SearchIndexBuilder.build(prefixInfos, infos, parallel);

		for (int i = 0; i < searchables.size(); i++) {
			PrefixedSearchable<IListElementInfo<?>, IListElement<?>> searchable = searchables.get(i);
			this.prefixedSearchables.put(prefixInfos.get(i), searchable);
			this.combinedSearchables.addSearchable(searchable);
		}
	}
}
