package net.md_5.bungee.scheduler;

import net.md_5.bungee.api.plugin.DummyPlugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.api.scheduler.TaskScheduler;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class SchedulerTest {

    @Test
    public void testRun() throws InterruptedException {
        TaskScheduler scheduler = new BungeeScheduler();

        final CountDownLatch latch = new CountDownLatch(1);

        scheduler.runAsync(DummyPlugin.INSTANCE, latch::countDown);

        latch.await(5, TimeUnit.SECONDS);

        assertEquals(0, latch.getCount());
    }

    @Test
    public void testCancel() throws InterruptedException {
        TaskScheduler scheduler = new BungeeScheduler();
        AtomicBoolean b = new AtomicBoolean();

        ScheduledTask task = setup(scheduler, b);
        scheduler.cancel(task.getId());
        Thread.sleep(250);
        assertFalse(b.get());

        task = setup(scheduler, b);
        scheduler.cancel(task);
        Thread.sleep(250);
        assertFalse(b.get());

        task = setup(scheduler, b);
        scheduler.cancel(task.getOwner());
        Thread.sleep(250);
        assertFalse(b.get());
    }

    @Test
    public void testScheduleAndRepeat() throws InterruptedException {
        TaskScheduler scheduler = new BungeeScheduler();
        AtomicBoolean b = new AtomicBoolean();

        setup(scheduler, b);
        Thread.sleep(250);
        assertTrue(b.get());

        b.set(false);
        Thread.sleep(250);
        assertTrue(b.get());
    }

    private ScheduledTask setup(TaskScheduler scheduler, AtomicBoolean hasRun) {
        return scheduler.schedule(DummyPlugin.INSTANCE, new Runnable() {

            @Override
            public void run() {
                hasRun.set(true);
            }
        }, 100, 100, TimeUnit.MILLISECONDS);
    }
}
