package com.misanthropy.fastjei;

import com.mojang.logging.LogUtils;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(Fastjei.MODID)
public class Fastjei {
	public static final String MODID = "fastjei";
	public static final Logger LOGGER = LogUtils.getLogger();

	public static volatile boolean concurrentSafetySetsInstalled = false;

	public static volatile int resourceGeneration;

	public Fastjei(FMLJavaModLoadingContext context) {
		context.registerConfig(ModConfig.Type.CLIENT, FastJeiConfig.SPEC);

		if (FMLEnvironment.dist == Dist.CLIENT) {
			LOGGER.info("[FastJEI] Loaded on client - JEI optimizations active");
			context.getModEventBus().addListener((FMLClientSetupEvent event) ->
					event.enqueueWork(Fastjei::setupJeiPatches)
			);
			context.getModEventBus().addListener((RegisterClientReloadListenersEvent event) ->
					event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> resourceGeneration++)
			);
		} else {
			LOGGER.info("[FastJEI] Loaded on dedicated server - nothing to do (client-only mod)");
		}
	}

	private static void setupJeiPatches() {
		JeiGeneration generation = JeiGeneration.detect();
		if (generation == JeiGeneration.UNSUPPORTED) {
			LOGGER.warn("[FastJEI] No compatibility layer matches the installed JEI - FastJEI is doing nothing this run");
			return;
		}
		JeiCompat compat = loadSupport(generation);
		if (compat == null) {
			return;
		}
		compat.validateMixinTargets();
		compat.installConcurrentSafetySets();
	}

	private static JeiCompat loadSupport(JeiGeneration generation) {
		try {
			Class<?> supportClass = Class.forName(generation.supportClass(), true, Fastjei.class.getClassLoader());
			return (JeiCompat) supportClass.getDeclaredConstructor().newInstance();
		} catch (Throwable t) {
			LOGGER.error("[FastJEI] Could not load the {} compatibility layer", generation, t);
			return null;
		}
	}
}
