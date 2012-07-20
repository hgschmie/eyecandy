package com.nesscomputing.eyecandy.servlet;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Inject;
import com.nesscomputing.eyecandy.common.GeoData;
import com.nesscomputing.eyecandy.common.GeoDataService;
import com.nesscomputing.eyecandy.consumer.GeoConverter;

public class GeoDataServlet extends RemoteServiceServlet implements GeoDataService
{
    private static final long serialVersionUID = 1L;

    private final GeoConverter geoConverter;

    @Inject
    public GeoDataServlet(final GeoConverter geoConverter)
    {
        this.geoConverter = geoConverter;
    }

    @Override
    public GeoData getGeoData()
    {
        return geoConverter.getGeoData();
    }
}
