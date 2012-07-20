package com.nesscomputing.eyecandy;

import java.util.concurrent.BlockingQueue;

import com.google.inject.Inject;
import com.nesscomputing.event.NessEvent;
import com.nesscomputing.event.NessEventReceiver;
import com.nesscomputing.event.NessEventTypes;
import com.nesscomputing.eyecandy.consumer.GeoConverter;

public class EyecandyReceiver implements NessEventReceiver
{
    private final BlockingQueue<NessEvent> queue;

    @Inject
    EyecandyReceiver(final BlockingQueue<NessEvent> queue)
    {
        this.queue = queue;
    }

    @Override
    public boolean accept(NessEvent event)
    {
        return event != null
               && (NessEventTypes.SEARCH.equals(event.getType())
                  || GeoConverter.IOS_APPLICATION_BECAME_ACTIVE.equals(event.getType()));
    }

    @Override
    public void receive(final NessEvent event)
    {
        queue.offer(event);
    }
}
