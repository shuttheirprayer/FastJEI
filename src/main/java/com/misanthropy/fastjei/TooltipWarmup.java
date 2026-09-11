package com.misanthropy.fastjei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.registries.ForgeRegistries;

public final class TooltipWarmup {
	private TooltipWarmup() {}

	public static void run() {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		TooltipFlag flag = minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
		try {
			if (player != null) {
				for (Attribute attribute : ForgeRegistries.ATTRIBUTES) {
					AttributeInstance instance = player.getAttribute(attribute);
					if (instance != null) {
						instance.getValue();
						for (AttributeModifier.Operation operation : AttributeModifier.Operation.values()) {
							instance.getModifiers(operation);
						}
					}
				}
			}
			new ItemStack(Items.STONE).getTooltipLines(player, flag);
		} catch (Throwable t) {
			Fastjei.LOGGER.debug("[FastJEI] Tooltip warm-up failed, lazily initialised tooltip listeners may race: {}", t.toString());
		}
	}
}
