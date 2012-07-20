package com.nesscomputing.eyecandy.common;

import com.google.gwt.user.client.rpc.IsSerializable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("EQ_COMPARETO_USE_OBJECT_EQUALS")
public class GeoLocation implements IsSerializable, Comparable<GeoLocation>
{

    private int id;
    private double lat;
    private double lng;
    private String eventName;
    private long count;
    private long time;
    private String eventType;

    public GeoLocation() {
    }

    public GeoLocation(final int id,
            final double lat,
            final double lng,
            final String eventName,
            final String eventType,
            final long count,
            final long time)
    {
        this.id = id;
        this.lat = lat;
        this.lng = lng;
        this.eventName = eventName;
        this.eventType = eventType;
        this.count = count;
        this.time = time;
    }

    public void setLat(double lat)
    {
        this.lat = lat;
    }

    public void setLng(double lng)
    {
        this.lng = lng;
    }

    public void setEventName(String eventName)
    {
        this.eventName = eventName;
    }

    public void setEventType(String eventType)
    {
        this.eventType = eventType;
    }

    public double getLat()
    {
        return lat;
    }

    public double getLng()
    {
        return lng;
    }

    public String getEventName()
    {
        return eventName;
    }

    public String getEventType()
    {
        return eventType;
    }

    public long getCount()
    {
        return count;
    }

    public int getId()
    {
        return id;
    }

    public long getTime()
    {
        return time;
    }

    @Override
    public int compareTo(final GeoLocation o) {
        if (o == null) {
            throw new NullPointerException();
        }

        if (o.getTime() < getTime()) {
            return -1;
        }
        else if (o.getTime() > getTime()) {
            return 1;
        }
        return 0;
    }
}
