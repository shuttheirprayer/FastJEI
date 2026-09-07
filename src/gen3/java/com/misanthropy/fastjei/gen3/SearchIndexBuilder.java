package com.misanthropy.fastjei.gen3;

import com.misanthropy.fastjei.FastJeiConfig;
import com.misanthropy.fastjei.Fastjei;
import com.misanthropy.fastjei.WorkerPool;
import mezz.jei.api.search.ISearchStorage;
import mezz.jei.api.search.ISearchStorageBuilder;
import mezz.jei.common.search.PrefixInfo;
import mezz.jei.common.search.PrefixedSearchable;
import mezz.jei.common.search.SearchMode;
import mezz.jei.gui.ingredients.IListElement;
import mezz.jei.gui.ingredients.IListElementInfo;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

public final class SearchIndexBuilder {
	private SearchIndexBuilder() {}

	private static final String TOOLTIP_PREFIX_ID = "tooltip";

	public static List<PrefixedSearchable<IListElementInfo<?>, IListElement<?>>> build(
			List<PrefixInfo<IListElementInfo<?>, IListElement<?>>> prefixInfos,
			Collection<IListElementInfo<?>> infos,
			boolean parallel
	) {
		long start = System.nanoTime();

		int count = prefixInfos.size();
		List<ISearchStorageBuilder<IListElement<?>>> builders = new ArrayList<>(count);
		for (PrefixInfo<IListElementInfo<?>, IListElement<?>> prefixInfo : prefixInfos) {
			builders.add(prefixInfo.createStorageBuilder());
		}

		boolean parallelTooltips = parallel && FastJeiConfig.PARALLEL_TOOLTIPS && Fastjei.concurrentSafetySetsInstalled
				&& prefixInfos.stream().anyMatch(SearchIndexBuilder::isTooltipPrefix);
		ISearchStorage<IListElement<?>>[] storages = newStorageArray(count);

		if (parallel) {
			try {
				WorkerPool.get().submit(() ->
						IntStream.range(0, count).parallel().forEach(i ->
								storages[i] = buildOne(prefixInfos.get(i), builders.get(i), infos, parallelTooltips)
						)
				).get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("[FastJEI] Is interrupted while building JEI search index..", e);
			} catch (ExecutionException e) {
				Fastjei.LOGGER.error("[FastJEI] Parallel search index build failed - rebuilding..", e.getCause());
				for (int i = 0; i < count; i++) {
					if (storages[i] == null) {
						PrefixInfo<IListElementInfo<?>, IListElement<?>> prefixInfo = prefixInfos.get(i);
						storages[i] = buildOne(prefixInfo, prefixInfo.createStorageBuilder(), infos, false);
					}
				}
			}
		} else {
			for (int i = 0; i < count; i++) {
				storages[i] = buildOne(prefixInfos.get(i), builders.get(i), infos, false);
			}
		}

		List<PrefixedSearchable<IListElementInfo<?>, IListElement<?>>> searchables = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			searchables.add(new PrefixedSearchable<>(storages[i], prefixInfos.get(i)));
		}

		if (FastJeiConfig.TIMINGS) {
			Fastjei.LOGGER.info(
					"[FastJEI] Built search indexes for {} ingredients x {} prefixes in {} ms (parallel: {}, parallel tooltips: {})",
					infos.size(),
					count,
					(System.nanoTime() - start) / 1_000_000,
					parallel,
					parallelTooltips
			);
		}
		return searchables;
	}

	private static boolean isTooltipPrefix(PrefixInfo<IListElementInfo<?>, IListElement<?>> prefixInfo) {
		return prefixInfo.toString().contains(TOOLTIP_PREFIX_ID);
	}

	@SuppressWarnings("unchecked")
	private static ISearchStorage<IListElement<?>>[] newStorageArray(int size) {
		return new ISearchStorage[size];
	}

	private static ISearchStorage<IListElement<?>> buildOne(
			PrefixInfo<IListElementInfo<?>, IListElement<?>> prefixInfo,
			ISearchStorageBuilder<IListElement<?>> builder,
			Collection<IListElementInfo<?>> infos,
			boolean parallelTooltips
	) {
		if (prefixInfo.getMode() != SearchMode.DISABLED) {
			if (parallelTooltips && isTooltipPrefix(prefixInfo)) {
				fillTokenizedInParallel(prefixInfo, builder, infos);
			} else {
				fillSequentially(prefixInfo, builder, infos);
			}
		}
		return builder.build();
	}

	private static void fillSequentially(
			PrefixInfo<IListElementInfo<?>, IListElement<?>> prefixInfo,
			ISearchStorageBuilder<IListElement<?>> builder,
			Collection<IListElementInfo<?>> infos
	) {
		for (IListElementInfo<?> info : infos) {
			IListElement<?> element = info.getElement();
			for (String string : safeGetStrings(prefixInfo, info)) {
				putIfNotBlank(builder, string, element);
			}
		}
	}

	private static void fillTokenizedInParallel(
			PrefixInfo<IListElementInfo<?>, IListElement<?>> prefixInfo,
			ISearchStorageBuilder<IListElement<?>> builder,
			Collection<IListElementInfo<?>> infos
	) {
		List<Map.Entry<IListElement<?>, Collection<String>>> tokenized = infos.parallelStream()
				.<Map.Entry<IListElement<?>, Collection<String>>>map(info -> new AbstractMap.SimpleImmutableEntry<>(
						info.getElement(),
						safeGetStrings(prefixInfo, info)
				))
				.toList();
		for (Map.Entry<IListElement<?>, Collection<String>> entry : tokenized) {
			IListElement<?> element = entry.getKey();
			for (String string : entry.getValue()) {
				putIfNotBlank(builder, string, element);
			}
		}
	}

	private static Collection<String> safeGetStrings(
			PrefixInfo<IListElementInfo<?>, IListElement<?>> prefixInfo,
			IListElementInfo<?> info
	) {
		try {
			return prefixInfo.getStrings(info);
		} catch (Throwable t) {
			Fastjei.LOGGER.debug("[FastJEI] getStrings failed for an ingredient, skipping it for this search prefix: {}", t.toString());
			return Collections.emptyList();
		}
	}

	private static void putIfNotBlank(ISearchStorageBuilder<IListElement<?>> builder, String string, IListElement<?> element) {
		String trimmed = string.trim();
		if (!trimmed.isEmpty()) {
			builder.put(trimmed, element);
		}
	}
}
