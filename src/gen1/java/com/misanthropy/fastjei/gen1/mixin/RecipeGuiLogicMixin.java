package com.misanthropy.fastjei.gen1.mixin;

import com.misanthropy.fastjei.FastJeiConfig;
import com.misanthropy.fastjei.Fastjei;
import com.misanthropy.fastjei.gen1.LazyRecipeLayoutList;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.common.config.RecipeSorterStage;
import mezz.jei.gui.recipes.IRecipeLayoutWithButtonsFactory;
import mezz.jei.gui.recipes.RecipeGuiLogic;
import mezz.jei.gui.recipes.layouts.IRecipeLayoutList;
import mezz.jei.gui.recipes.lookups.IFocusedRecipes;
import mezz.jei.gui.recipes.lookups.ILookupState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;

@Mixin(value = RecipeGuiLogic.class, remap = false)
public abstract class RecipeGuiLogicMixin {

	@Shadow
	@Final
	private IRecipeManager recipeManager;

	@Shadow
	@Final
	private IRecipeLayoutWithButtonsFactory recipeLayoutFactory;

	@Shadow
	private ILookupState state;

	@Inject(method = "createRecipeLayoutsWithButtons", at = @At("HEAD"), cancellable = true)
	private <T> void fastjei$lazyLayouts(
			Set<RecipeSorterStage> recipeSorterStages,
			IFocusedRecipes<T> selectedRecipes,
			@Nullable AbstractContainerMenu container,
			@Nullable Player player,
			CallbackInfoReturnable<IRecipeLayoutList> cir
	) {
		if (!FastJeiConfig.LAZY_LAYOUTS) {
			return;
		}
		List<T> recipes = selectedRecipes.getRecipes();
		if (recipes.size() <= FastJeiConfig.LAZY_LAYOUT_THRESHOLD) {
			return;
		}

		if (FastJeiConfig.TIMINGS) {
			Fastjei.LOGGER.info(
					"[FastJEI] Category '{}' has {} recipes - using lazy per-page recipe layouts",
					selectedRecipes.getRecipeCategory().getRecipeType().getUid(),
					recipes.size()
			);
		}
		cir.setReturnValue(new LazyRecipeLayoutList<>(
				recipeManager,
				recipeLayoutFactory,
				selectedRecipes.getRecipeCategory(),
				recipes,
				state.getFocuses()
		));
	}
}
