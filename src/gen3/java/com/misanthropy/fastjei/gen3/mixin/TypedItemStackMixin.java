package com.misanthropy.fastjei.gen3.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.misanthropy.fastjei.gen3.CachedStack;
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
	private volatile CachedStack fastjei$cached;

	@WrapMethod(method = "getIngredient()Lnet/minecraft/world/item/ItemStack;", require = 0)
	private ItemStack fastjei$keepStack(Operation<ItemStack> original) {
		CachedStack cached = fastjei$cached;
		if (cached == null || !cached.untouched()) {
			cached = CachedStack.of(createItemStackUncached());
			fastjei$cached = cached;
		}
		return cached.stack();
	}
}
