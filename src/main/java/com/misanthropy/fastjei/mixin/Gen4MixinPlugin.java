package com.misanthropy.fastjei.mixin;

import com.misanthropy.fastjei.JeiGeneration;

public final class Gen4MixinPlugin extends GenerationMixinPlugin {

	@Override
	protected JeiGeneration generation() {
		return JeiGeneration.GEN4;
	}
}
