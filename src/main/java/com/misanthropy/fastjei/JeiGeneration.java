package com.misanthropy.fastjei;

import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum JeiGeneration {
	GEN1("com.misanthropy.fastjei.gen1.Gen1Support"),
	GEN2("com.misanthropy.fastjei.gen2.Gen2Support"),
	GEN3("com.misanthropy.fastjei.gen3.Gen3Support"),
	UNSUPPORTED(null);

	private static final Logger LOGGER = LogManager.getLogger("FastJEI");

	private static final int[] GEN1_MIN = {15, 20, 0, 113};
	private static final int[] GEN2_MIN = {15, 24, 0, 150};
	private static final int[] GEN3_MIN = {15, 48, 0, 179};

	private static final int[] LAZY_LAYOUT_MAX_EXCLUSIVE = {15, 24, 0, 150};

	private static volatile JeiGeneration detected;
	private static volatile String detectedVersion;

	private final String supportClass;

	JeiGeneration(String supportClass) {
		this.supportClass = supportClass;
	}

	public String supportClass() {
		return supportClass;
	}

	public static JeiGeneration detect() {
		JeiGeneration result = detected;
		if (result != null) {
			return result;
		}
		synchronized (JeiGeneration.class) {
			if (detected != null) {
				return detected;
			}
			String version = findJeiVersion();
			detectedVersion = version;
			JeiGeneration generation = classify(version);
			if (version == null) {
				LOGGER.warn("[FastJEI] Could not determine the installed JEI version - all optimizations will be disabled.");
			} else if (generation == UNSUPPORTED) {
				LOGGER.warn("[FastJEI] JEI {} is older than the oldest supported build (15.20.0.113) - all optimizations stay disabled.. Use latest beta please.", version);
			} else {
				LOGGER.info("[FastJEI] Detected JEI {} - using the {} compatibility layer", version, generation);
			}
			detected = generation;
			return generation;
		}
	}

	public static String jeiVersion() {
		detect();
		return detectedVersion;
	}

	public static boolean supportsLazyRecipeLayouts() {
		String version = jeiVersion();
		return version != null && compare(parse(version), LAZY_LAYOUT_MAX_EXCLUSIVE) < 0 && detect() != UNSUPPORTED;
	}

	private static JeiGeneration classify(String version) {
		if (version == null) {
			return UNSUPPORTED;
		}
		int[] parsed = parse(version);
		if (compare(parsed, GEN3_MIN) >= 0) {
			return GEN3;
		}
		if (compare(parsed, GEN2_MIN) >= 0) {
			return GEN2;
		}
		if (compare(parsed, GEN1_MIN) >= 0) {
			return GEN1;
		}
		return UNSUPPORTED;
	}

	private static String findJeiVersion() {
		try {
			LoadingModList list = LoadingModList.get();
			if (list == null) {
				return null;
			}
			for (IModInfo info : list.getMods()) {
				if ("jei".equals(info.getModId())) {
					return info.getVersion().toString();
				}
			}
		} catch (Throwable t) {
			LOGGER.warn("[FastJEI] Failed to read the JEI version from the mod list", t);
		}
		return null;
	}

	private static int[] parse(String version) {
		String[] parts = version.split("[.\\-+]");
		int[] numbers = new int[parts.length];
		for (int i = 0; i < parts.length; i++) {
			int end = 0;
			while (end < parts[i].length() && Character.isDigit(parts[i].charAt(end))) {
				end++;
			}
			numbers[i] = end == 0 ? -1 : Integer.parseInt(parts[i].substring(0, end));
		}
		return numbers;
	}

	private static int compare(int[] left, int[] right) {
		int length = Math.max(left.length, right.length);
		for (int i = 0; i < length; i++) {
			int a = i < left.length ? left[i] : 0;
			int b = i < right.length ? right[i] : 0;
			if (a != b) {
				return Integer.compare(a, b);
			}
		}
		return 0;
	}
}
