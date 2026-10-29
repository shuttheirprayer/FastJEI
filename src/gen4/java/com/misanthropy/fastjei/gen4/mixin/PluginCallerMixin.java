package com.misanthropy.fastjei.gen4.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.misanthropy.fastjei.FastJeiConfig;
import com.misanthropy.fastjei.gen4.AsyncJeiStart;
import mezz.jei.api.IModPlugin;
import mezz.jei.library.load.PluginCaller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

@Mixin(value = PluginCaller.class, remap = false)
public abstract class PluginCallerMixin {

	@WrapOperation(
			method = "callOnPlugins",
			at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
	private static void fastjei$pinnedPluginsOnRenderThread(Consumer<Object> func, Object plugin, Operation<Void> original) {
		if (AsyncJeiStart.inWorker() && FastJeiConfig.isRenderThreadPlugin(((IModPlugin) plugin).getPluginUid())) {
			AsyncJeiStart.onRenderThread(() -> original.call(func, plugin));
			return;
		}
		original.call(func, plugin);
	}
}
