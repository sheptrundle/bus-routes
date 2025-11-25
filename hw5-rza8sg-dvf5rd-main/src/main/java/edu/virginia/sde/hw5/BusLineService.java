package edu.virginia.sde.hw5;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class BusLineService {
    private final DatabaseDriver databaseDriver;

    public BusLineService(DatabaseDriver databaseDriver) {
        this.databaseDriver = databaseDriver;
    }

    public void addStops(List<Stop> stops) {
        try {
            databaseDriver.connect();
            databaseDriver.addStops(stops);
            databaseDriver.disconnect();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addBusLines(List<BusLine> busLines) {
        try {
            databaseDriver.connect();
            databaseDriver.addBusLines(busLines);
            databaseDriver.disconnect();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<BusLine> getBusLines() {
        try {
            databaseDriver.connect();
            var busLines = databaseDriver.getBusLines();
            databaseDriver.disconnect();
            return busLines;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Stop> getStops() {
        try {
            databaseDriver.connect();
            var stops = databaseDriver.getAllStops();
            databaseDriver.disconnect();
            return stops;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Route getRoute(BusLine busLine) {
        try {
            databaseDriver.connect();
            var stops = databaseDriver.getRouteForBusLine(busLine);
            databaseDriver.disconnect();
            return stops;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the closest stop to a given coordinate (using Euclidean distance, not great circle distance)
     * @param latitude - North/South coordinate (positive is North, Negative is South) in degrees
     * @param longitude - East/West coordinate (negative is West, Positive is East) in degrees
     * @return the closest Stop
     */
    public Stop getClosestStop(double latitude, double longitude) {
        try {
            var stops = databaseDriver.getAllStops();
            if (stops.isEmpty()) {
                return null;
            }
            Stop closestStop = null;
            double closestDistance = Double.MAX_VALUE;
            for(Stop stop : stops){
                double distance = stop.distanceTo(latitude, longitude);
                if (distance < closestDistance){
                    closestStop = stop;
                    closestDistance = distance;
                }
            }
            return closestStop;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Given two stop, a source and a destination, find the shortest (by distance) BusLine that starts
     * from source and ends at Destination.
     * @return Optional.empty() if no bus route visits both points
     * @throws IllegalArgumentException if either stop doesn't exist in the database
     */
    public Optional<BusLine> getRecommendedBusLine(Stop source, Stop destination) {
        try {
            var busLines = databaseDriver.getBusLines();
            BusLine recommendedBusLine = null;
            double shortestDistance = Double.MAX_VALUE;
            for(BusLine line : busLines){
                Route route = line.getRoute();
                var stops = route.getStops();
                if (!stops.contains(source) || !stops.contains(destination)) continue; //skip line if doesn't contain both stops

                int sourceIndex = stops.indexOf(source);
                int destinationIndex = stops.indexOf(destination);

                int currentIndex = sourceIndex;
                double distance = 0.0;

                while (currentIndex != destinationIndex) {
                    int nextIndex = (currentIndex + 1) % stops.size(); //loop
                    Stop currentStop = stops.get(currentIndex);
                    Stop nextStop = stops.get(nextIndex);
                    distance += currentStop.distanceTo(nextStop);
                    currentIndex = nextIndex;
                }

                if (distance < shortestDistance) {
                    shortestDistance = distance;
                    recommendedBusLine = line;
                }
            }
            if(recommendedBusLine == null) return Optional.empty();
            else return Optional.of(recommendedBusLine);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
