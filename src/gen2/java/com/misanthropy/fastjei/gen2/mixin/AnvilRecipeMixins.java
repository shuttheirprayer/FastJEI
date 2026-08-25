package com.misanthropy.fastjei.gen2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.misanthropy.fastjei.FastJeiConfig;
import com.misanthropy.fastjei.MenuUpdateSuppressor;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import mezz.jei.library.plugins.vanilla.anvil.AnvilHelper;
import mezz.jei.library.plugins.vanilla.anvil.AnvilRecipeMaker;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.stream.Stream;

public final class AnvilRecipeMixins {

	private AnvilRecipeMixins() {}

	@Mixin(value = AnvilRecipeMaker.class, remap = false)
	public static abstract class SkipGeneratedEnchantments {

		@WrapMethod(method = "getBookEnchantmentRecipes()Ljava/util/stream/Stream;")
		private Stream<IJeiAnvilRecipe> fastjei$skipBookEnchantments(Operation<Stream<IJeiAnvilRecipe>> original) {
			if (FastJeiConfig.SKIP_GENERATED_ENCHANTMENT_RECIPES) {
				return Stream.empty();
			}
			return original.call();
		}
	}

	@Mixin(value = AnvilRecipeMaker.class, remap = false)
	public static abstract class SkipGeneratedRepairs {

		@WrapMethod(method = "getRepairRecipes()Ljava/util/stream/Stream;")
		private Stream<IJeiAnvilRecipe> fastjei$skipRepairs(Operation<Stream<IJeiAnvilRecipe>> original) {
			if (FastJeiConfig.SKIP_GENERATED_REPAIR_RECIPES) {
				return Stream.empty();
			}
			return original.call();
		}
	}

	@Mixin(value = AnvilHelper.class, remap = false)
	public static abstract class QuietAnvilFill {

		@WrapMethod(method = "setAnvilMenu")
		private static AnvilMenu fastjei$fillQuietly(
				AnvilMenu menu,
				ItemStack left,
				ItemStack right,
				Operation<AnvilMenu> original
		) {
			if (!FastJeiConfig.SKIP_REDUNDANT_MENU_UPDATES) {
				return original.call(menu, left, right);
			}
			MenuUpdateSuppressor.suppress(menu);
			try {
				return original.call(menu, left, right);
			} finally {
				MenuUpdateSuppressor.release();
				menu.createResult();
			}
		}
	}

	@Mixin(ItemCombinerMenu.class)
	public static abstract class SkipSuppressedSlotUpdates {

		@Inject(method = "slotsChanged", at = @At("HEAD"), cancellable = true)
		private void fastjei$skipWhileFilling(Container container, CallbackInfo ci) {
			if (MenuUpdateSuppressor.isSuppressed((ItemCombinerMenu) (Object) this)) {
				ci.cancel();
			}
		}
	}
}
