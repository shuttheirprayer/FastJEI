package com.misanthropy.fastjei.gen3;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record CachedStack(ItemStack stack, int count, @Nullable CompoundTag tag) {

	public static CachedStack of(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return new CachedStack(stack, stack.getCount(), tag == null ? null : tag.copy());
	}

	public boolean untouched() {
		try {
			return stack.getCount() == count && Objects.equals(stack.getTag(), tag);
		} catch (RuntimeException e) {
			return false;
		}
	}
}
