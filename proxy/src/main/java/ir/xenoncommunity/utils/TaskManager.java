package ir.xenoncommunity.utils;

import ir.xenoncommunity.XenonCore;

import java.util.concurrent.*;

@SuppressWarnings("unused")
public class TaskManager {
    private final ExecutorService queueExecutorService;
    private final ExecutorService executorService;
    private final ExecutorService cachedExecutorService;
    private final ScheduledExecutorService scheduledExecutor;

    public TaskManager() {
        this.queueExecutorService = new ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.cachedExecutorService = Executors.newCachedThreadPool();
        this.scheduledExecutor = Executors.newScheduledThreadPool(4);
    }

    public void add(final Runnable runnableIn) {
        queueExecutorService.submit(() -> {
            try {
                runnableIn.run();
            } catch (final Exception e) {
                XenonCore.instance.getLogger().error(e.getMessage());
            } finally {
                Thread.currentThread().interrupt();
            }
        });
    }

    public void async(final Runnable runnableIn) {
        executorService.submit(() -> {
            try {
                runnableIn.run();
            } catch (final Exception e) {
                XenonCore.instance.getLogger().error(e.getMessage());
            }
        });
    }

    public void cachedAsync(final Runnable runnableIn) {
        cachedExecutorService.submit(() -> {
            try {
                runnableIn.run();
            } catch (final Exception e) {
                XenonCore.instance.getLogger().error(e.getMessage());
            }
        });
    }

    public ScheduledFuture<?> repeatingTask(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return scheduledExecutor.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    public void shutdown() {
        executorService.shutdown();
        cachedExecutorService.shutdown();
        scheduledExecutor.shutdown();
    }
}