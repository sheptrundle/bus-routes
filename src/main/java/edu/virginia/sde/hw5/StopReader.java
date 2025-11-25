package edu.virginia.sde.hw5;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class StopReader {

    private final URL busStopsApiUrl;

    public StopReader(Configuration configuration) {
        this.busStopsApiUrl = configuration.getBusStopsURL();
    }

    /**
     * Read all the stops from the "stops" json URL from Configuration Reader
     * @return List of stops
     */
    public List<Stop> getStops() {
        List<Stop> stopList = new ArrayList<>();
        var webServiceReader = new WebServiceReader(busStopsApiUrl);
        var jsonRoot = webServiceReader.getJSONObject();
        // Create array with each stop
        // Understanding JSON object and array generation from Stack Overflow (link below):
        // https://stackoverflow.com/questions/32624166/how-to-get-json-array-within-json-object
        JSONArray stopsArray = jsonRoot.getJSONArray("stops");
        for (int i = 0; i < stopsArray.length(); i++) {
            // Access array of all stop info
            JSONObject JSONstop = stopsArray.getJSONObject(i);
            int ID = JSONstop.getInt("id");
            String name = JSONstop.getString("name");

            // Access array of lat/long
            JSONArray position = JSONstop.getJSONArray("position");
            double latitude = position.getDouble(0);
            double longitude = position.getDouble(1);

            // Add to list of stops
            stopList.add(new Stop(ID, name, latitude, longitude));
        }
        return stopList;
    }
}
