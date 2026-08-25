package com.misanthropy.fastjei;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

public final class WorkerPool {
	private WorkerPool() {}

	private static volatile ForkJoinPool pool;

	public static ForkJoinPool get() {
		ForkJoinPool p = pool;
		if (p != null) {
			return p;
		}
		synchronized (WorkerPool.class) {
			if (pool != null) {
				return pool;
			}
			int workers = FastJeiConfig.effectiveWorkers();
			final ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
			pool = new ForkJoinPool(
					workers,
					fjp -> {
						ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(fjp);
						if (contextLoader != null) {
							thread.setContextClassLoader(contextLoader);
						}
						thread.setName("FastJEI-Worker-" + thread.getPoolIndex());
						thread.setDaemon(true);
						thread.setPriority(Thread.NORM_PRIORITY - 1);
						return thread;
					},
					null,
					false
			);
			Fastjei.LOGGER.info("[FastJEI] Started worker pool with {} threads", workers);
			return pool;
		}
	}
}
