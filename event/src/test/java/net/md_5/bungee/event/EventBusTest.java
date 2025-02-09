package net.md_5.bungee.event;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EventBusTest {

    private final EventBus bus = new EventBus();
    private final CountDownLatch latch = new CountDownLatch(2);

    @Test
    public void testNestedEvents() {
        bus.register(this);
        bus.post(new FirstEvent(), null); // Waterfall - We dont need an exception handler here
        assertEquals(0, latch.getCount());
    }

    @EventHandler
    public void firstListener(FirstEvent event) {
        bus.post(new SecondEvent(), null); // Waterfall - We dont need an exception handler here
        assertEquals(1, latch.getCount());
        latch.countDown();
    }

    @EventHandler
    public void secondListener(SecondEvent event) {
        latch.countDown();
    }

    public static class FirstEvent {
    }

    public static class SecondEvent {
    }

}
