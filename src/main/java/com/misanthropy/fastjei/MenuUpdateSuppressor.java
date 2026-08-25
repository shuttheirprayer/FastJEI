package com.misanthropy.fastjei;

import net.minecraft.world.inventory.AbstractContainerMenu;

public final class MenuUpdateSuppressor {

	private static AbstractContainerMenu suppressed;

	private MenuUpdateSuppressor() {}

	public static void suppress(AbstractContainerMenu menu) {
		suppressed = menu;
	}

	public static void release() {
		suppressed = null;
	}

	public static boolean isSuppressed(AbstractContainerMenu menu) {
		return suppressed == menu;
	}
}
