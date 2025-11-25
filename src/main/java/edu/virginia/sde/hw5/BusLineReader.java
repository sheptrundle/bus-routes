package edu.virginia.sde.hw5;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class BusLineReader {
    private final URL busLinesApiUrl;
    private final URL busStopsApiUrl;

    /* You'll need this to get the Stop objects when building the Routes object */
    private final StopReader stopReader;
    /**
     * Returns a list of BusLine objects. This is a "deep" list, meaning all the BusLine objects
     * already have their Route objects fully populated with that line's Stops.
     */

    public BusLineReader(Configuration configuration) {
        this.busStopsApiUrl = configuration.getBusStopsURL();
        this.busLinesApiUrl = configuration.getBusLinesURL();
        stopReader = new StopReader(configuration);
    }

    /**
     * This method returns the BusLines from the API service, including their
     * complete Routes.
     */
    public List<BusLine> getBusLines() {
        List<BusLine> busLines = new ArrayList<>();
        // jsonRoot for lines
        var webServiceReaderLines = new WebServiceReader(busLinesApiUrl);
        var jsonRootLines = webServiceReaderLines.getJSONObject();

        // Loop through all BusLines
        JSONArray linesArray = jsonRootLines.getJSONArray("lines");
        for (int i = 0; i < linesArray.length(); i++) {
            JSONObject line = linesArray.getJSONObject(i);

            // Get basic information
            int id = line.getInt("id");
            boolean isActive = line.getBoolean("is_active");
            String longName = line.getString("long_name");
            String shortName = line.getString("short_name");
            Route route = getRoute(id);

            busLines.add(new BusLine(id, isActive, longName, shortName, route));
        }
        return busLines;
    }


    // Helper method for forming routes
    private Route getRoute(int busID) {
        Route route = new Route();
        // jsonRoot for stops
        var webServiceReaderStops = new WebServiceReader(busStopsApiUrl);
        var jsonRootStops = webServiceReaderStops.getJSONObject();

        // List of all Stop objects
        List<Stop> stops = stopReader.getStops();

        // Loop through all routes
        JSONArray routes = jsonRootStops.getJSONArray("routes");
        for (int j = 0; j < routes.length(); j++) {
            JSONObject currentRoute = routes.getJSONObject(j);
            int currentRouteId = currentRoute.getInt("id");
            // Found route with same ID as bus line
            if (currentRouteId == busID) {
                JSONArray stopIDs = currentRoute.getJSONArray("stops");
                // Loop through ID's of stops on route
                for (int k = 0; k < stopIDs.length(); k++) {
                    int stopID = stopIDs.getInt(k);
                    // Loop through Stops in stopList
                    for (Stop stop : stops) {
                        // Found Stop object, add and break
                        if (stop.getId() == stopID) {
                            route.add(stop);
                            break;
                        }
                    }
                }
            }
        }
        return route;
    }
}
