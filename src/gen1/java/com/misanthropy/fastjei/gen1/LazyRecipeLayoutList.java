package com.misanthropy.fastjei.gen1;

import com.misanthropy.fastjei.Fastjei;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.common.Internal;
import mezz.jei.common.gui.elements.DrawableNineSliceTexture;
import mezz.jei.gui.recipes.IRecipeLayoutWithButtonsFactory;
import mezz.jei.gui.recipes.RecipeLayoutWithButtons;
import mezz.jei.gui.recipes.layouts.IRecipeLayoutList;
import mezz.jei.gui.recipes.layouts.RecipeLayoutDrawableErrored;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class LazyRecipeLayoutList<T> implements IRecipeLayoutList {
	private static final int RECIPE_BORDER_PADDING = 4;
	private static final int MAX_CACHED_LAYOUTS = 512;

	private final IRecipeManager recipeManager;
	private final IRecipeLayoutWithButtonsFactory factory;
	private final IRecipeCategory<T> recipeCategory;
	private final List<T> recipes;
	private final IFocusGroup focuses;

	private final Map<Integer, RecipeLayoutWithButtons<T>> cache = new LinkedHashMap<>(64, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Integer, RecipeLayoutWithButtons<T>> eldest) {
			return size() > MAX_CACHED_LAYOUTS;
		}
	};

	public LazyRecipeLayoutList(
			IRecipeManager recipeManager,
			IRecipeLayoutWithButtonsFactory factory,
			IRecipeCategory<T> recipeCategory,
			List<T> recipes,
			IFocusGroup focuses
	) {
		this.recipeManager = recipeManager;
		this.factory = factory;
		this.recipeCategory = recipeCategory;
		this.recipes = recipes;
		this.focuses = focuses;
	}

	@Override
	public int size() {
		return recipes.size();
	}

	@Override
	public @NonNull List<RecipeLayoutWithButtons<?>> subList(int from, int to) {
		List<RecipeLayoutWithButtons<?>> page = new ArrayList<>(to - from);
		for (int i = from; i < to; i++) {
			page.add(get(i));
		}
		return page;
	}

	@Override
	public @NonNull Optional<RecipeLayoutWithButtons<?>> findFirst() {
		if (recipes.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(get(0));
	}

	@Override
	public void tick() {
	}

	private RecipeLayoutWithButtons<T> get(int index) {
		return cache.computeIfAbsent(index, i -> {
			T recipe = recipes.get(i);
			DrawableNineSliceTexture background = Internal.getTextures().getRecipeBackground();
			try {
				IRecipeLayoutDrawable<T> drawable = recipeManager
						.createRecipeLayoutDrawable(recipeCategory, recipe, focuses, background, RECIPE_BORDER_PADDING)
						.orElseGet(() -> new RecipeLayoutDrawableErrored<>(recipeCategory, recipe, background, RECIPE_BORDER_PADDING));
				return factory.create(drawable);
			} catch (RuntimeException | LinkageError e) {
				Fastjei.LOGGER.error("[FastJEI] Failed to create a recipe layout or its buttons, showing it as error.", e);
				return factory.create(new RecipeLayoutDrawableErrored<>(recipeCategory, recipe, background, RECIPE_BORDER_PADDING));
			}
		});
	}
}
