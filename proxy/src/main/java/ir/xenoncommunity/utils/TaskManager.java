package ir.xenoncommunity.utils;

import ir.xenoncommunity.XenonCore;

import java.util.concurrent.*;

@SuppressWarnings("unused")
public class TaskManager {
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;

    public TaskManager() {
        this.executorService = new ThreadPoolExecutor(
                2 ,
                4,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());

        this.scheduledExecutorService = Executors.newScheduledThreadPool(4);
    }

    public synchronized void add(final Runnable runnableIn) {
        executorService.submit(() -> {
            try {
                runnableIn.run();
            } catch (final Exception e) {
                XenonCore.instance.getLogger().error(e.getMessage());
            } finally {
                Thread.currentThread().interrupt();
            }
        });
    }

    public synchronized void repeatingTask(final Runnable runnableIn, final int initDelay, final int delayInMS, final TimeUnit timeUnit) {
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                runnableIn.run();
            } catch (final Exception e) {
                XenonCore.instance.getLogger().error(e.getMessage());
            } finally {
                Thread.currentThread().interrupt();
            }
        }, initDelay, delayInMS, timeUnit);
    }
    public synchronized void independentTask(final Runnable task) {
        new Thread(() -> {
            try {
                task.run();
            } catch (final Exception e) {
                XenonCore.instance.getLogger().error(e.getMessage());
            } finally {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}
