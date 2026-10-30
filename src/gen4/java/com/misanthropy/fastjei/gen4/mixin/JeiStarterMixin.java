package com.misanthropy.fastjei.gen4.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.misanthropy.fastjei.FastJeiConfig;
import com.misanthropy.fastjei.gen4.AsyncJeiStart;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.common.Internal;
import mezz.jei.library.startup.IStopCallback;
import mezz.jei.library.startup.JeiStarter;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mixin(value = JeiStarter.class, remap = false)
public abstract class JeiStarterMixin {

	@Shadow
	private boolean running;

	@Shadow
	@Final
	private List<IStopCallback> stopCallbacks;

	@Shadow
	public abstract void start();

	@Inject(method = "start", at = @At("HEAD"), cancellable = true)
	private void fastjei$startInBackground(CallbackInfo ci) {
		if (!FastJeiConfig.ASYNC_START || AsyncJeiStart.inWorker() || AsyncJeiStart.syncFallback()) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.getOverlay() != null) {
			return;
		}
		ci.cancel();
		AsyncJeiStart.launch(this::start, this::fastjei$abandon, () -> running);
	}

	@Inject(method = "stop", at = @At("HEAD"))
	private void fastjei$waitForBackgroundStart(CallbackInfo ci) {
		AsyncJeiStart.cancelAndWait();
	}

	@Unique
	private void fastjei$abandon() {
		if (running) {
			return;
		}
		List<IStopCallback> callbacks = new ArrayList<>(stopCallbacks);
		stopCallbacks.clear();
		for (IStopCallback callback : callbacks) {
			try {
				callback.onRuntimeStopped();
			} catch (RuntimeException ignored) {
			}
		}
	}

	@WrapOperation(
			method = "start",
			at = @At(value = "INVOKE", target = "Lmezz/jei/library/load/PluginCaller;callOnPlugins(Ljava/lang/String;Ljava/util/List;Ljava/util/function/Consumer;)V", ordinal = 2))
	private void fastjei$sendRuntimeOnRenderThread(String title, List<IModPlugin> plugins, Consumer<IModPlugin> func, Operation<Void> original) {
		AsyncJeiStart.onRenderThread(() -> original.call(title, plugins, func));
	}

	@WrapOperation(
			method = "start",
			at = @At(value = "INVOKE", target = "Lmezz/jei/common/Internal;setRuntime(Lmezz/jei/api/runtime/IJeiRuntime;)V"))
	private void fastjei$setRuntimeOnRenderThread(IJeiRuntime runtime, Operation<Void> original) {
		AsyncJeiStart.onRenderThread(() -> original.call(runtime));
	}

	@WrapOperation(
			method = "start",
			at = @At(value = "INVOKE", target = "Lmezz/jei/library/startup/JeiStarter;verifyClientRecipes(Lnet/minecraft/client/Minecraft;)V"))
	private void fastjei$verifyOnRenderThread(JeiStarter self, Minecraft minecraft, Operation<Void> original) {
		try {
			AsyncJeiStart.onRenderThread(() -> original.call(self, minecraft));
		} catch (AsyncJeiStart.Abort ignored) {
		}
	}
}
