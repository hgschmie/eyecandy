package com.nesscomputing.eyecandy.common;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Set;

public class GeoData implements IsSerializable {

    private Set<GeoLocation> geoLocations;
    private long count;

    /**
     * GWT requires a non-args c'tor.
     */
    public GeoData() {
    }

    public GeoData(final Set<GeoLocation> geoLocations, final long count) {
        this.geoLocations = geoLocations;
        this.count = count;
    }

    public Set<GeoLocation> getGeoLocations() {
        return geoLocations;
    }

    public long getCount() {
        return count;
    }

}

