package com.misanthropy.fastjei;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class BrewingRecipeIndex {

	private BrewingRecipeIndex() {}

	private static final Map<Object, Object> INDEX = new HashMap<>();

	private static boolean active;
	private static boolean brokenThisSession;

	public static boolean usable(Collection<?> recipes) {
		if (brokenThisSession) {
			return false;
		}
		if (recipes.isEmpty()) {
			INDEX.clear();
			active = FastJeiConfig.INDEXED_BREWING_LOOKUP;
		}
		if (!active) {
			return false;
		}
		int indexed = INDEX.size();
		int held = recipes.size();
		if (indexed != held) {
			brokenThisSession = true;
			active = false;
			INDEX.clear();
			Fastjei.LOGGER.warn("[FastJEI] The brewing recipe index fell out of step with JEI's recipe set " +
							"({} indexed vs {} held). Falling back to JEI's own lookup for the rest of this session.",
					indexed, held);
			return false;
		}
		return true;
	}

	public static Object find(Object recipe) {
		return INDEX.get(recipe);
	}

	public static void added(Object recipe) {
		if (active) {
			INDEX.putIfAbsent(recipe, recipe);
		}
	}

	public static void removed(Object recipe) {
		if (active) {
			INDEX.remove(recipe);
		}
	}
}
