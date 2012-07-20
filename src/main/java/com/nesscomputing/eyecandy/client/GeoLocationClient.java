package com.nesscomputing.eyecandy.client;

import com.nesscomputing.eyecandy.common.GeoData;
import com.nesscomputing.eyecandy.common.GeoDataServiceAsync;
import com.nesscomputing.eyecandy.common.GeoDataServiceAsync.Util;
import com.nesscomputing.eyecandy.common.GeoLocation;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.maps.client.MapType;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.Maps;
import com.google.gwt.maps.client.control.LargeMapControl;
import com.google.gwt.maps.client.control.MapTypeControl;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.geom.Point;
import com.google.gwt.maps.client.geom.Size;
import com.google.gwt.maps.client.overlay.Icon;
import com.google.gwt.maps.client.overlay.Marker;
import com.google.gwt.maps.client.overlay.MarkerOptions;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.gwt.user.client.ui.RootPanel;

import java.util.Iterator;
import java.util.Set;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class GeoLocationClient implements EntryPoint {

    public static final int LOOPTIME = 5000;
    public static final int MARKER_COUNT = 200;
    public static final int MAX_LIFETIME = 30*1000;
    // public static final int MARKER_DELAY = LOOPTIME / MARKER_COUNT;

    private static final String [] MARKER = new String [] {
        "/icons/red-on-48.png",
        "/icons/yellow-on-48.png",
        "/icons/green-on-48.png",
        "/icons/blue-on-48.png",
        "/icons/amber-on-48.png",
        "/icons/uv-on-48.png",
        "/icons/white-on-48.png",
    };

    private static final int [] BOUNDARY = new int [] {
        0,
        2,
        5,
        10,
        20
    };

    private static final int [] ICON_SIZE = new int [] {
        20,
        26,
        32,
        38,
        44
    };

    public static class TimedMarker
    {
        public Marker marker;
        public long timestamp;
    }

    // markerList and markerPos are a simple wrapping FIFO to record the
    // locations of the markers
    private TimedMarker [] markerList = new TimedMarker[MARKER_COUNT];
    private int markerPos;

    // Sequence marker for the last sequence served. If we get the same
    // data set twice, we ignore it.
    private long currSequence = -1;

    // True if populate is running.
    private boolean running = false;

    private GeoDataServiceAsync geoDataService = null;

    private MapWidget map;

    private int names = 0;
    private final Grid grid = new Grid(30, 2);
    private final PopupPanel scrollPanel = new PopupPanel(false);
    private int currLeft = -1;

    public void onModuleLoad() {
        Maps.loadMapsApi("AIzaSyAcesZ9S4RONQSOKqAAKqMbE7BsbhvsN0s", "2", false, new Runnable() {
            public void run() {
                buildUi();
            }
        });
    }

    void buildUi()
    {
        geoDataService = Util.getInstance();
        markerPos = markerList.length;

        int zoom = fetchParameter("zoom", 11);
        names = fetchParameter("names", 0);
        double lat = fetchParameter("lat", 37.5);
        double lng = fetchParameter("lng", -121.8);


        LatLng home = LatLng.newInstance(lat, lng);
        map = new MapWidget(home, zoom);
        map.setScrollWheelZoomEnabled(false);
        map.setSize("100%", "100%");
        map.setCurrentMapType(MapType.getHybridMap());
        map.addControl(new LargeMapControl());
        map.addControl(new MapTypeControl());

        RootPanel.get("map").add(map);

        final PopupPanel panel = new LabelPopup(new Label("Lat: " + home.getLatitude() + ", Lng: " + home.getLongitude() + ", Zoom: " + zoom));
        panel.show();

        if (names != 0)  {
            grid.setStylePrimaryName("city-table");
            scrollPanel.setSize("10px", "10px");
            scrollPanel.setWidget(grid);
            scrollPanel.setPopupPositionAndShow(new PositionCallback() {
                @Override
                public void setPosition(int offsetWidth, int offsetHeight) {
                    currLeft = (Window.getClientWidth() - offsetWidth) - 10;
                    scrollPanel.setPopupPosition(currLeft, 100);
                }
            });
        }

        execute(10000, new Runnable() {
            @Override
            public void run() { panel.hide(); }
        });

        populate();
        expireMarkers();
    }

    private void expireMarkers()
    {
        final long now = System.currentTimeMillis();
        for (int i = 0; i < markerList.length; i++) {
            final TimedMarker marker = markerList[i];
            if (marker != null && now > marker.timestamp) {
                map.removeOverlay(markerList[i].marker);
                markerList[i] = null;
            }
        }

        execute(MAX_LIFETIME/10, new Runnable() {
            @Override
            public void run() {
                expireMarkers();
            }
        });
    }

    private int fetchParameter(final String name, final int defaultValue) {
        try {
            return Integer.parseInt(Window.Location.getParameter(name));
        }
        catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    private double fetchParameter(final String name, final double defaultValue) {
        try {
            final String val = Window.Location.getParameter(name);
            return val != null ? Double.parseDouble(val) : defaultValue;
        }
        catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    public void populate() {

        AsyncCallback<GeoData> callback = new AsyncCallback<GeoData> () {

            // Communication with the mothership failed. Show a popup and wait for a few
            // seconds.
            public void onFailure(Throwable caught) {
                final PopupPanel panel = new LabelPopup(new Label("Communication failure!"));
                panel.setPopupPositionAndShow(new PositionCallback() {

                    @Override
                    public void setPosition(int offsetWidth, int offsetHeight) {
                        int left = (Window.getClientWidth() - offsetWidth) / 3;
                        int top = (Window.getClientHeight() - offsetHeight) / 3;
                        panel.setPopupPosition(left, top);
                    }
                });

                execute(2000, new Runnable() {
                    @Override
                    public void run() {
                        panel.hide();
                        populate();
                    }
                });
            }

            public void onSuccess(GeoData geoData) {

                if (running || (geoData.getCount() == currSequence)) {
                    final PopupPanel panel = new LabelPopup(new Label("Ignoring Duplicate: " + geoData.getCount() + " / " + running));
                    panel.setPopupPosition(100, 0);
                    panel.show();

                    execute(LOOPTIME/5, new Runnable() {
                        @Override
                        public void run() {
                            panel.hide();
                            populate();
                        }
                    });
                }
                else {
                    currSequence = geoData.getCount();

                    final PopupPanel panel = new LabelPopup(new Label("Processing: " + geoData.getCount()+ " (" + geoData.getGeoLocations().size() + " Events)"));
                    panel.setPopupPosition(100, 0);
                    panel.show();

                    execute(1000, new Runnable() {
                        @Override
                        public void run() {
                            panel.hide();
                        }
                    });

                    execute(LOOPTIME, new Runnable() {
                        @Override
                        public void run() {
                            populate();
                        }
                    });

                    running = true;
                    updateDisplay(geoData.getGeoLocations());
                    running = false;
                }
            }
        };

        geoDataService.getGeoData(callback);
    }

    public void updateDisplay(Set<GeoLocation> locations)
    {
        long loopTime = System.currentTimeMillis();

        Iterator<GeoLocation> it = locations.iterator();
        if (it.hasNext()) {
            int delay = LOOPTIME / locations.size();
            fireUpdate(it, loopTime, delay);
        }
    }

    public void fireUpdate(final Iterator<GeoLocation> it, final long loopTime, final int delay)
    {

        if (markerList[--markerPos] != null) {
            map.removeOverlay(markerList[markerPos].marker);
        }
        final GeoLocation location = it.next();

        markerList[markerPos] = buildMarker(location);
        map.addOverlay(markerList[markerPos].marker);

        if (names != 0) {
            updateGrid(location);
        }

        if (markerPos == 0) {
            markerPos = markerList.length;
        }

        if (it.hasNext()) {
            execute(delay, new Runnable() {
                @Override
                public void run() {
                    fireUpdate(it, loopTime, delay);
                }
            });
        }
    }

    private TimedMarker buildMarker(final GeoLocation location)
    {
        LatLng markerPos = LatLng.newInstance(location.getLat(), location.getLng());

        final Icon icon;

        if (location.getId() < MARKER.length) {
            icon = Icon.newInstance(MARKER[location.getId()]);
        }
        else {
            icon = Icon.newInstance(MARKER[0]);
        }

        int size = 0;
        for (int i = 0; i < BOUNDARY.length; i++)
        {
            if (location.getCount() > BOUNDARY[i]) {
                size = ICON_SIZE[i];
            }
        }

        icon.setIconSize(Size.newInstance(size, size));
        icon.setIconAnchor(Point.newInstance(size/2, size/2));

        MarkerOptions options = MarkerOptions.newInstance();
        options.setTitle(location.getEventName() + " (" + location.getEventType() + " / " + location.getCount() + ")");
        options.setIcon(icon);
        TimedMarker timedMarker = new TimedMarker();
        timedMarker.marker =  new Marker(markerPos, options);
        timedMarker.timestamp = System.currentTimeMillis() + MAX_LIFETIME;
        return timedMarker;
    }

    private Timer execute(final int delay, final Runnable code)
    {
        final Timer t = new Timer() {
                @Override
                public void run() {
                    code.run();
                }
            };

        t.schedule(delay);
        return t;
    }

    private void updateGrid(final GeoLocation location)
    {
        if (!"".equals(location.getEventName())) {
            grid.removeRow(29);
            grid.insertRow(0);
            grid.setText(0, 0, location.getEventName());
            grid.setText(0, 1, location.getEventType());

            int left = (Window.getClientWidth() - scrollPanel.getOffsetWidth());

            // Avoid jittering. Allow the window to grow to the left and only shrink
            // if we would gain at least 100 px.
            if ((left < currLeft) || ((left - currLeft) > 100)) {
                currLeft = left;
                scrollPanel.setPopupPosition(left, 100);
            }
        }
    }

    public static class LabelPopup extends PopupPanel
    {
        public LabelPopup(final Label label) {
            super(true);
            setWidget(label);
        }
    }

}
