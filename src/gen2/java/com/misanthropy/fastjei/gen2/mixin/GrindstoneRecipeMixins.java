package com.misanthropy.fastjei.gen2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.misanthropy.fastjei.FastJeiConfig;
import com.misanthropy.fastjei.MenuUpdateSuppressor;
import mezz.jei.api.recipe.vanilla.IJeiGrindstoneRecipe;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.common.platform.IPlatformRecipeHelper;
import mezz.jei.forge.platform.RecipeHelper;
import mezz.jei.library.plugins.vanilla.grindstone.GrindstoneRecipeMaker;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.stream.Stream;

public final class GrindstoneRecipeMixins {

	private GrindstoneRecipeMixins() {}

	@Mixin(value = GrindstoneRecipeMaker.class, remap = false)
	public static abstract class SkipGeneratedDisenchanting {

		@WrapMethod(method = "getDisenchantRecipes")
		private static Stream<IJeiGrindstoneRecipe> fastjei$skipDisenchanting(
				IPlatformRecipeHelper recipeHelper,
				GrindstoneMenu menu,
				Operation<Stream<IJeiGrindstoneRecipe>> original
		) {
			if (FastJeiConfig.SKIP_GENERATED_ENCHANTMENT_RECIPES) {
				return Stream.empty();
			}
			return original.call(recipeHelper, menu);
		}
	}

	@Mixin(value = GrindstoneRecipeMaker.class, remap = false)
	public static abstract class SkipGeneratedRepairs {

		@WrapMethod(method = "getRepairRecipes")
		private static Stream<IJeiGrindstoneRecipe> fastjei$skipRepairs(
				IPlatformRecipeHelper recipeHelper,
				IIngredientManager ingredientManager,
				GrindstoneMenu menu,
				Operation<Stream<IJeiGrindstoneRecipe>> original
		) {
			if (FastJeiConfig.SKIP_GENERATED_REPAIR_RECIPES) {
				return Stream.empty();
			}
			return original.call(recipeHelper, ingredientManager, menu);
		}
	}

	@Mixin(value = RecipeHelper.class, remap = false)
	public static abstract class QuietGrindstoneFill {

		@WrapMethod(method = "getGrindstoneResult")
		private ItemStack fastjei$fillQuietly(
				GrindstoneMenu menu,
				ItemStack left,
				ItemStack right,
				Operation<ItemStack> original
		) {
			if (!FastJeiConfig.SKIP_REDUNDANT_MENU_UPDATES) {
				return original.call(menu, left, right);
			}
			MenuUpdateSuppressor.suppress(menu);
			try {
				return original.call(menu, left, right);
			} finally {
				MenuUpdateSuppressor.release();
			}
		}
	}

	@Mixin(GrindstoneMenu.class)
	public static abstract class SkipSuppressedSlotUpdates {

		@Inject(method = "slotsChanged", at = @At("HEAD"), cancellable = true)
		private void fastjei$skipWhileFilling(Container container, CallbackInfo ci) {
			if (MenuUpdateSuppressor.isSuppressed((GrindstoneMenu) (Object) this)) {
				ci.cancel();
			}
		}
	}
}
