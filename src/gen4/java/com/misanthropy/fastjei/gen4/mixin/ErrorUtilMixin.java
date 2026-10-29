package com.misanthropy.fastjei.gen4.mixin;

import com.misanthropy.fastjei.gen4.AsyncJeiStart;
import mezz.jei.common.util.ErrorUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ErrorUtil.class, remap = false)
public abstract class ErrorUtilMixin {

	@Inject(method = "assertMainThread", at = @At("HEAD"), cancellable = true)
	private static void fastjei$startThreadCountsAsMain(CallbackInfo ci) {
		if (AsyncJeiStart.inWorker()) {
			ci.cancel();
		}
	}
}
