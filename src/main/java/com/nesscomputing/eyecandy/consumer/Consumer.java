package com.nesscomputing.eyecandy.consumer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.nesscomputing.event.NessEvent;
import com.nesscomputing.event.NessEventType;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.lifecycle.guice.OnStage;
import com.nesscomputing.logging.Log;

public class Consumer
{

    private static final Log LOG = Log.findLog();

    private final ExecutorService executors = Executors.newCachedThreadPool();
    private final QueueLeecher queueLeecher;

    @Inject
    public Consumer(final BlockingQueue<NessEvent> queue)
    {
        this.queueLeecher = new QueueLeecher(queue);
    }

    @OnStage(LifecycleStage.START)
    public void start()
    {
        executors.submit(queueLeecher);
    }

    @OnStage(LifecycleStage.STOP)
    public void stop()
    {
        executors.shutdown();
    }

    public Multimap<NessEventType, NessEvent> getEvents()
    {
        return queueLeecher.getEvents();
    }

    public static class QueueLeecher implements Runnable
    {
        private final BlockingQueue<NessEvent> queue;

        private final AtomicBoolean running = new AtomicBoolean();

        private final AtomicReference<Multimap<NessEventType, NessEvent>> events = new AtomicReference<Multimap<NessEventType, NessEvent>>(null);

        public QueueLeecher(final BlockingQueue<NessEvent> queue)
        {
            this.queue = queue;
        }

        public Multimap<NessEventType, NessEvent> getEvents()
        {
            return events.getAndSet(LinkedListMultimap.<NessEventType, NessEvent>create());
        }

        @Override
        public void run() {

            running.set(true);
            events.set(LinkedListMultimap.<NessEventType, NessEvent>create());

            while (running.get()) {
                NessEvent event = null;
                try {
                    event = queue.poll(10, TimeUnit.SECONDS);

                    if (event == null) {
                        LOG.debug("Tick...");
                    }
                    else {
                        LOG.debug("Got event from queue: %s", event);

                        final Multimap<NessEventType, NessEvent> map = events.get();
                        if (map != null) {
                            map.put(event.getType(), event);
                        }
                    }
                }
                catch (InterruptedException ie) {
                    LOG.info("Queue Leecher was interrupted, shutting down!");
                    running.set(false);
                    // Set the interrupt status again.
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
