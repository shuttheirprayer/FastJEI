package com.misanthropy.fastjei;

public interface JeiCompat {

	String[] mixinTargets();

	void installConcurrentSafetySets();

	default void validateMixinTargets() {
		ClassLoader loader = getClass().getClassLoader();
		boolean allOk = true;
		for (String target : mixinTargets()) {
			try {
				Class.forName(target, false, loader);
			} catch (Throwable t) {
				allOk = false;
				Fastjei.LOGGER.error("[FastJEI] Failed to apply mixins to {} - this JEI version is probably not supported. Probably." +
						"FastJEI optimizations for this class will not work. Because PROBABLY you got the wrong JEI version.", target, t);
			}
		}
		if (allOk) {
			Fastjei.LOGGER.info("[FastJEI] All JEI mixin targets loaded and patched successfully!");
		}
	}
}
