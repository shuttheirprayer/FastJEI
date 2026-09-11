package com.misanthropy.fastjei;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

public final class TooltipWarmup {
	private TooltipWarmup() {}

	public static void run() {
		Minecraft minecraft = Minecraft.getInstance();
		TooltipFlag flag = minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
		try {
			new ItemStack(Items.STONE).getTooltipLines(minecraft.player, flag);
		} catch (Throwable t) {
			Fastjei.LOGGER.debug("[FastJEI] Tooltip warm-up failed, lazily initialised tooltip listeners may race: {}", t.toString());
		}
	}
}
