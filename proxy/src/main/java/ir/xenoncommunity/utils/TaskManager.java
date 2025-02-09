package ir.xenoncommunity.utils;

import ir.xenoncommunity.XenonCore;

import java.util.concurrent.*;

@SuppressWarnings("unused")
public class TaskManager {
    /**
     * Declare all required variables
     */
    private final ExecutorService queueExecutorService;
    private final ExecutorService executorService;
    private final ExecutorService cachedExecutorService;
    private final ScheduledExecutorService scheduledExecutor;

    /**
     * Initialize all required variables
     */
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

    /**
     * add a task into execution list
     *
     * @param runnableIn a runnable to be run by task
     */
    public void add(Runnable runnableIn) {
        queueExecutorService.submit(() -> {
            try {
                runnableIn.run();
            } catch (Exception e) {
                XenonCore.instance.getLogger().error(e.getMessage());
            } finally {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * run a task asynchronously
     *
     * @param runnableIn a runnable to be run by task
     */
    public void async(Runnable runnableIn) {
        executorService.submit(() -> {
            try {
                runnableIn.run();
            } catch (Exception e) {
                XenonCore.instance.getLogger().error(e.getMessage());
            }
        });
    }

    /**
     * run a task asynchronously with cachedExecution
     *
     * @param runnableIn a runnable to be run by task
     */
    public void cachedAsync(Runnable runnableIn) {
        cachedExecutorService.submit(() -> {
            try {
                runnableIn.run();
            } catch (Exception e) {
                XenonCore.instance.getLogger().error(e.getMessage());
            }
        });
    }

    /**
     * run a repeating task
     *
     * @param task         a runnable to be run by task
     * @param initialDelay initial delay of loop
     * @param period       period of loop
     * @param unit         time unit of time variables
     * @return
     */
    public ScheduledFuture<?> repeatingTask(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return scheduledExecutor.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    /**
     * Shuts down all executors
     */
    public void shutdown() {
        executorService.shutdown();
        cachedExecutorService.shutdown();
        scheduledExecutor.shutdown();
    }
}