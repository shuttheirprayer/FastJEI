package com.misanthropy.fastjei.common.mixin;

import com.google.common.cache.LoadingCache;
import com.misanthropy.fastjei.Fastjei;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "mezz.jei.library.render.batch.ItemStackBatchRendererCache", remap = false)
public abstract class ItemStackBatchRendererCacheMixin {

	@Shadow
	@Final
	private LoadingCache<?, ?> cache;

	@Unique
	private int fastjei$resourceGeneration = Fastjei.resourceGeneration;

	@Inject(method = "renderBatch", at = @At("HEAD"))
	private void fastjei$dropModelsFromOldResources(CallbackInfo ci) {
		int generation = Fastjei.resourceGeneration;
		if (generation != fastjei$resourceGeneration) {
			fastjei$resourceGeneration = generation;
			cache.invalidateAll();
		}
	}
}
