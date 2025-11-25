package edu.virginia.sde.hw5;

import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws SQLException {
        try {
            Configuration configuration = new Configuration();
            BusLineReader readBus = new BusLineReader(configuration);
            StopReader readStop = new StopReader(configuration);
            DatabaseDriver driver = new DatabaseDriver(configuration);
            driver.connect();
            driver.clearTables();
            driver.createTables();
            driver.addStops(readStop.getStops());
            driver.commit();
            driver.addBusLines(readBus.getBusLines());
            driver.commit();
            driver.disconnect();
        } catch(SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
