package com.misanthropy.fastjei.gen3.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(targets = {
		"mezz.jei.common.ingredients.itemStacks.TypedItemStack",
		"mezz.jei.library.ingredients.itemStacks.TypedItemStack"
}, remap = false)
public abstract class TypedItemStackMixin {

	@Shadow
	protected abstract ItemStack createItemStackUncached();

	@Unique
	private volatile ItemStack fastjei$stack;

	@WrapMethod(method = "getIngredient()Lnet/minecraft/world/item/ItemStack;", require = 0)
	private ItemStack fastjei$keepStack(Operation<ItemStack> original) {
		ItemStack stack = fastjei$stack;
		if (stack == null) {
			stack = createItemStackUncached();
			fastjei$stack = stack;
		}
		return stack;
	}
}
