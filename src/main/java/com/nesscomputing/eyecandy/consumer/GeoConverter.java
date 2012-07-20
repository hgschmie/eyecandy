package com.nesscomputing.eyecandy.consumer;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.nesscomputing.event.NessEvent;
import com.nesscomputing.event.NessEventType;
import com.nesscomputing.event.NessEventTypes;
import com.nesscomputing.eyecandy.common.GeoData;
import com.nesscomputing.eyecandy.common.GeoLocation;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.lifecycle.guice.OnStage;
import com.nesscomputing.logging.Log;

public class GeoConverter
{
    private static final Log LOG = Log.findLog();

    public static final NessEventType IOS_APPLICATION_BECAME_ACTIVE = NessEventType.getForName("IOS_APPLICATION_BECAME_ACTIVE");

    private final long cycle = 5000L;

    private final Consumer consumer;
    private final Timer timer;
    private final AtomicLong counter = new AtomicLong(1L);
    private final AtomicInteger idCnt = new AtomicInteger(0);

    private final AtomicReference<GeoData> geoData = new AtomicReference<GeoData>(null);


    @Inject
    public GeoConverter(final Consumer consumer)
    {
        this.consumer = consumer;
        this.timer = new Timer("IP Converter");
        this.geoData.set(new GeoData(new TreeSet<GeoLocation>(), counter.getAndIncrement()));
    }

    @OnStage(LifecycleStage.START)
    public void start()
    {
        timer.schedule(new IPCompressorTask(), cycle, cycle);
    }

    @OnStage(LifecycleStage.STOP)
    public void stop()
    {
        timer.cancel();
    }

    public GeoData getGeoData() {
        return geoData.get();
    }

    private void processEvents(final String type, final int id, final Collection<NessEvent> nessEvents, final Set<GeoLocation> geoLocations) {

        for (NessEvent nessEvent : nessEvents) {
            final Map<String, ? extends Object> payload = nessEvent.getPayload();
            final Object l1 = payload.get("lat");
            final Object l2 = payload.get("lon");
            if (l1 != null && l2 != null) {
                final double lat;
                final double lng;

                if (l1 instanceof Double) {
                    lat = Double.class.cast(l1);
                }
                else {
                    lat = Double.parseDouble(l1.toString());
                }
                if (l2 instanceof Double) {
                    lng = Double.class.cast(l2);
                }
                else {
                    lng = Double.parseDouble(l2.toString());
                }

                final GeoLocation geoLocation = new GeoLocation(id,
                        lat,
                        lng,
                        "",
                        type,
                        1,
                        nessEvent.getTimestamp().getMillis());
                geoLocations.add(geoLocation);
            }
            else {
                LOG.debug("Ignored event without location: %s", nessEvent);
            }
        }
    }

    private class IPCompressorTask extends TimerTask {

        @Override
        public void run() {
            try {
                final Multimap<NessEventType, NessEvent> events = consumer.getEvents();

                final Set<GeoLocation> newEvents = Sets.newHashSet();

                final Collection<NessEvent> search = events.get(NessEventTypes.SEARCH);
                if (search != null) {
                    LOG.info("IN: %d Search events", search.size());
                    processEvents("search", 0, search, newEvents);
                }

                final Collection<NessEvent> active = events.get(IOS_APPLICATION_BECAME_ACTIVE);
                if (active != null) {
                    LOG.info("IN: %d Activation Events", active.size());
                    processEvents("activate", 1, active, newEvents);
                }

                final GeoData newGeoData = new GeoData(newEvents, counter.getAndIncrement());
                geoData.set(newGeoData);
                LOG.info("OUT: %d: %d Geo Events", newGeoData.getCount(), newGeoData.getGeoLocations().size());
            } catch (Exception e) {
                LOG.warn(e, "Timer Task caught Exception");
            }
        }
    }
}



