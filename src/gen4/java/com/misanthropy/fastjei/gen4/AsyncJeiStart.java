package com.misanthropy.fastjei.gen4;

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

public final class AsyncJeiStart {
	private static final Logger LOGGER = LogManager.getLogger("FastJEI");
	private static final ThreadLocal<Boolean> IN_WORKER = ThreadLocal.withInitial(() -> false);

	private static volatile Thread worker;
	private static volatile boolean cancelled;
	private static boolean syncFallback;

	private AsyncJeiStart() {}

	public static final class Abort extends Error {
		private Abort() {
			super("JEI start cancelled", null, false, false);
		}
	}

	public static boolean inWorker() {
		return IN_WORKER.get();
	}

	public static boolean syncFallback() {
		return syncFallback;
	}

	public static void launch(Runnable start, Runnable abandon, BooleanSupplier running) {
		cancelAndWait();
		cancelled = false;
		long begin = System.nanoTime();
		Thread thread = new Thread(() -> {
			IN_WORKER.set(true);
			try {
				start.run();
				LOGGER.info("[FastJEI] JEI started in the background in {} ms", (System.nanoTime() - begin) / 1_000_000);
			} catch (Abort e) {
				abandon.run();
				LOGGER.info("[FastJEI] Background JEI start cancelled");
			} catch (Throwable t) {
				LOGGER.error("[FastJEI] Background JEI start failed, starting it on the main thread instead", t);
				abandon.run();
				if (!cancelled) {
					Minecraft.getInstance().execute(() -> {
						if (cancelled || running.getAsBoolean()) {
							return;
						}
						syncFallback = true;
						try {
							start.run();
						} finally {
							syncFallback = false;
						}
					});
				}
			} finally {
				IN_WORKER.remove();
			}
		}, "FastJEI start");
		thread.setDaemon(true);
		worker = thread;
		thread.start();
	}

	public static void cancelAndWait() {
		Thread thread = worker;
		if (thread == null || thread == Thread.currentThread()) {
			return;
		}
		cancelled = true;
		boolean interrupted = false;
		while (thread.isAlive()) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				interrupted = true;
			}
		}
		worker = null;
		if (interrupted) {
			Thread.currentThread().interrupt();
		}
	}

	public static void onRenderThread(Runnable task) {
		if (!inWorker()) {
			task.run();
			return;
		}
		if (cancelled) {
			throw new Abort();
		}
		FutureTask<Void> future = new FutureTask<>(task, null);
		Minecraft.getInstance().execute(future);
		while (true) {
			try {
				future.get(50, TimeUnit.MILLISECONDS);
				return;
			} catch (TimeoutException e) {
				if (cancelled && future.cancel(false)) {
					throw new Abort();
				}
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				if (cause instanceof RuntimeException runtime) {
					throw runtime;
				}
				if (cause instanceof Error error) {
					throw error;
				}
				throw new RuntimeException(cause);
			} catch (InterruptedException e) {
				throw new Abort();
			} catch (CancellationException e) {
				throw new Abort();
			}
		}
	}
}
