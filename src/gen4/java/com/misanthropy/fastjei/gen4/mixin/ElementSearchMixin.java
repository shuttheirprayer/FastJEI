package com.misanthropy.fastjei.gen4.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.misanthropy.fastjei.FastJeiConfig;
import com.misanthropy.fastjei.gen3.SearchIndexBuilder;
import mezz.jei.common.search.PrefixInfo;
import mezz.jei.common.search.PrefixedSearchable;
import mezz.jei.gui.ingredients.IListElement;
import mezz.jei.gui.ingredients.IListElementInfo;
import mezz.jei.gui.search.ElementPrefixParser;
import mezz.jei.gui.search.ElementSearch;
import mezz.jei.gui.search.ElementSearchIndex;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mixin(value = ElementSearch.class, remap = false)
public abstract class ElementSearchMixin {

	@Unique
	private static final int FASTJEI_MIN_PARALLEL_BATCH = 128;

	@Shadow
	@Final
	private Map<PrefixInfo<IListElementInfo<?>, IListElement<?>>, PrefixedSearchable<IListElementInfo<?>, IListElement<?>>> prefixedSearchables;

	@Shadow
	@Final
	private ElementPrefixParser elementPrefixParser;

	@Unique
	private List<ElementSearchIndex> fastjei$deferredIndexes;

	@Unique
	private Collection<IListElementInfo<?>> fastjei$deferredInfos;

	@WrapOperation(
			method = {"<init>", "rebuildSearchIndexes"},
			at = @At(
					value = "INVOKE",
					target = "Lmezz/jei/gui/search/ElementSearch;rebuildSearchIndex(Lmezz/jei/gui/search/ElementSearchIndex;Ljava/util/Collection;)V"
			)
	)
	private void fastjei$deferIndex(
			ElementSearch search,
			ElementSearchIndex index,
			Collection<IListElementInfo<?>> infos,
			Operation<Void> original
	) {
		if (!FastJeiConfig.PARALLEL_BUILD) {
			original.call(search, index, infos);
			return;
		}
		if (this.fastjei$deferredIndexes == null) {
			this.fastjei$deferredIndexes = new ArrayList<>();
		}
		this.fastjei$deferredIndexes.add(index);
		this.fastjei$deferredInfos = infos;
	}

	@WrapOperation(
			method = {"<init>", "rebuildSearchIndexes"},
			at = @At(
					value = "INVOKE",
					target = "Lmezz/jei/gui/search/ElementSearch;updateCombinedSearchables()V"
			)
	)
	private void fastjei$buildDeferred(ElementSearch search, Operation<Void> original) {
		List<ElementSearchIndex> indexes = this.fastjei$deferredIndexes;
		Collection<IListElementInfo<?>> infos = this.fastjei$deferredInfos;
		this.fastjei$deferredIndexes = null;
		this.fastjei$deferredInfos = null;
		if (indexes != null) {
			List<PrefixInfo<IListElementInfo<?>, IListElement<?>>> prefixInfos = new ArrayList<>(indexes.size());
			for (ElementSearchIndex index : indexes) {
				prefixInfos.add(((ElementPrefixParserAccessor) this.elementPrefixParser).fastjei$getPrefixInfo(index));
			}
			List<PrefixedSearchable<IListElementInfo<?>, IListElement<?>>> searchables =
					SearchIndexBuilder.build(prefixInfos, infos, infos.size() >= FASTJEI_MIN_PARALLEL_BATCH);
			for (int i = 0; i < searchables.size(); i++) {
				this.prefixedSearchables.put(prefixInfos.get(i), searchables.get(i));
			}
		}
		original.call(search);
	}
}
