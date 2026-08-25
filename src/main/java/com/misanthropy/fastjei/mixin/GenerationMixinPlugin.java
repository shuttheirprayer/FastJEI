package com.misanthropy.fastjei.mixin;

import com.misanthropy.fastjei.JeiGeneration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.MixinService;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class GenerationMixinPlugin implements IMixinConfigPlugin {

	protected static final Logger LOGGER = LogManager.getLogger("FastJEI");

	public record RequiredMethod(String feature, String name, String descriptor) {
		public static RequiredMethod of(String feature, String name) {
			return new RequiredMethod(feature, name, null);
		}
	}

	private final Map<String, ClassNode> targetCache = new HashMap<>();

	protected abstract JeiGeneration generation();

	protected Map<String, RequiredMethod> requiredMethods() {
		return Map.of();
	}

	protected Map<String, String> configGates() {
		return Map.of();
	}

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (JeiGeneration.detect() != generation()) {
			return false;
		}
		String configKey = configGates().get(mixinClassName);
		if (configKey != null && Boolean.FALSE.equals(readClientFlag(configKey))) {
			LOGGER.info("[FastJEI] {} is false in fastjei-client.toml, so that mixin is not applied at all", configKey);
			return false;
		}
		RequiredMethod required = requiredMethods().get(mixinClassName);
		if (required == null) {
			return true;
		}
		ClassNode target = readTarget(targetClassName);
		if (target == null) {
			LOGGER.warn("[FastJEI] Turned off the {} optimization: this JEI build has no {}. " +
					"JEI keeps its normal behaviour here.", required.feature(), targetClassName);
			return false;
		}
		if (!declares(target, required)) {
			LOGGER.warn("[FastJEI] Turned off the {} optimization: {} in this JEI build does not declare {}{}. " +
							"JEI keeps its normal behaviour here.",
					required.feature(), targetClassName, required.name(),
					required.descriptor() == null ? "" : required.descriptor());
			return false;
		}
		return true;
	}

	private static boolean declares(ClassNode target, RequiredMethod required) {
		for (MethodNode method : target.methods) {
			if (method.name.equals(required.name())
					&& (required.descriptor() == null || method.desc.equals(required.descriptor()))) {
				return true;
			}
		}
		return false;
	}

	private static final Map<String, Boolean> CONFIG_FLAGS = new HashMap<>();

	private static Boolean readClientFlag(String key) {
		if (CONFIG_FLAGS.containsKey(key)) {
			return CONFIG_FLAGS.get(key);
		}
		Boolean value = null;
		try {
			Path path = FMLPaths.CONFIGDIR.get().resolve("fastjei-client.toml");
			if (Files.isReadable(path)) {
				for (String line : Files.readAllLines(path)) {
					String trimmed = line.trim();
					if (trimmed.startsWith("#")) {
						continue;
					}
					int equals = trimmed.indexOf('=');
					if (equals < 0 || !trimmed.substring(0, equals).trim().equals(key)) {
						continue;
					}
					String raw = trimmed.substring(equals + 1).trim();
					if ("true".equals(raw) || "false".equals(raw)) {
						value = Boolean.valueOf(raw);
					}
					break;
				}
			}
		} catch (Throwable t) {
			LOGGER.debug("[FastJEI] Could not pre-read {} from fastjei-client.toml", key, t);
		}
		CONFIG_FLAGS.put(key, value);
		return value;
	}

	private ClassNode readTarget(String targetClassName) {
		if (targetCache.containsKey(targetClassName)) {
			return targetCache.get(targetClassName);
		}
		ClassNode node = null;
		try {
			IClassBytecodeProvider provider = MixinService.getService().getBytecodeProvider();
			try {
				node = provider.getClassNode(targetClassName);
			} catch (ClassNotFoundException e) {
				node = provider.getClassNode(targetClassName.replace('.', '/'));
			}
		} catch (Exception | LinkageError e) {
			LOGGER.debug("[FastJEI] Could not read the mixin target class {}", targetClassName, e);
		}
		targetCache.put(targetClassName, node);
		return node;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
