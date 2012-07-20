package com.nesscomputing.eyecandy.common;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("geo")
public interface GeoDataService extends RemoteService
{
    GeoData getGeoData();
}

