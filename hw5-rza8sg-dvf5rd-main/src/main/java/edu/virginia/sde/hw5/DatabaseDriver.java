package edu.virginia.sde.hw5;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DatabaseDriver {
    private final String sqliteFilename;
    private Connection connection;

    public DatabaseDriver(Configuration configuration) {
        this.sqliteFilename = configuration.getDatabaseFilename();
    }

    public DatabaseDriver (String sqlListDatabaseFilename) {
        this.sqliteFilename = sqlListDatabaseFilename;
    }

    /**
     * Connect to a SQLite Database. This turns out Foreign Key enforcement, and disables auto-commits
     * @throws SQLException
     */
    public void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            throw new IllegalStateException("The connection is already opened");
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + sqliteFilename);
        //the next line enables foreign key enforcement - do not delete/comment out
        connection.createStatement().execute("PRAGMA foreign_keys = ON");
        //the next line disables auto-commit - do not delete/comment out
        connection.setAutoCommit(false);
    }

    /**
     * Commit all changes since the connection was opened OR since the last commit/rollback
     */
    public void commit() throws SQLException {
        connection.commit();
    }

    /**
     * Rollback to the last commit, or when the connection was opened
     */
    public void rollback() throws SQLException {
        connection.rollback();
    }

    /**
     * Ends the connection to the database
     */
    public void disconnect() throws SQLException {
        connection.close();
    }

    /**
     * Creates the three database tables Stops, BusLines, and Routes, with the appropriate constraints including
     * foreign keys, if they do not exist already. If they already exist, this method does nothing.
     * As a hint, you'll need to create Routes last, and Routes must include Foreign Keys to Stops and
     * BusLines.
     * @throws SQLException
     */
    public void createTables() throws SQLException {
        try {
            // Used ChatGPT 4o Mini for understanding Statement.execute
            // Prompt: How would I run a string of SQL commands in Java?
            Statement stmt = connection.createStatement();
            // Stops
            String createStops = """
                CREATE TABLE IF NOT EXISTS Stops(
                    ID INTEGER PRIMARY KEY,
                    StopName TEXT NOT NULL,
                    Latitude REAL NOT NULL,
                    Longitude REAL NOT NULL
                    );
                """;
            stmt.execute(createStops);

            // BusLines
            String createBusLines = """
                CREATE TABLE IF NOT EXISTS BusLines(
                    ID INTEGER PRIMARY KEY,
                    IsActive INTEGER NOT NULL,
                    LongName TEXT NOT NULL,
                    ShortName TEXT NOT NULL
                    );
                """;
            stmt.execute(createBusLines);

            // Routes
            String createRoutes = """
                CREATE TABLE IF NOT EXISTS Routes(
                    ID INTEGER PRIMARY KEY,
                    StopID INTEGER NOT NULL,
                    BusLineID INTEGER NOT NULL,
                    RouteOrder INTEGER NOT NULL,
                    FOREIGN KEY (StopID) REFERENCES Stops(id) ON DELETE CASCADE,
                    FOREIGN KEY (BusLineID) REFERENCES BusLines(id) ON DELETE CASCADE
                    );
                """;
            stmt.execute(createRoutes);

        } catch (SQLException e){
            throw new SQLException(e);
        }
    }

    /**
     * Add a list of Stops to the Database. After adding all the stops, the changes will be committed. However,
     * if any SQLExceptions occur, this method will rollback and throw the exception.
     * @param stops - the stops to be added to the database
     */
    public void addStops(List<Stop> stops) throws SQLException {
        try {
            String insertStopsSQL = "INSERT INTO Stops(ID, StopName, Latitude, Longitude) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(insertStopsSQL);

            for (Stop stop : stops) {
                stmt.setInt(1, stop.getId());
                stmt.setString(2, stop.getName());
                stmt.setDouble(3, stop.getLatitude());
                stmt.setDouble(4, stop.getLongitude());
                stmt.addBatch();
            }
            stmt.executeBatch();

        } catch (SQLException e) {
            rollback();
            throw e;
        }
    }

    /**
     * Gets a list of all Stops in the database
     */
    public List<Stop> getAllStops() throws SQLException {
        try {
            Statement stmt = connection.createStatement();
            // IntelliJ AI filled in this code up to while statement
            ResultSet rs = stmt.executeQuery("SELECT * FROM Stops");
            List<Stop> stops = new ArrayList<>();
            while (rs.next()) {
                Stop stop = new Stop(rs.getInt("ID"), rs.getString("StopName"), rs.getDouble("Latitude"), rs.getDouble("Longitude"));
                stops.add(stop);
            }
            return stops;
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }

    /**
     * Get a Stop by its ID number. Returns Optional.isEmpty() if no Stop matches the ID.
     */
    public Optional<Stop> getStopById(int stopId) throws SQLException {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM Stops WHERE ID = " + stopId);
            // Return Optional.isEmpty() if no matches
            if (!rs.next()) {
                return Optional.empty();
            }
            Stop stop = new Stop(rs.getInt("ID"), rs.getString("StopName"), rs.getDouble("Latitude"), rs.getDouble("Longitude"));
            return Optional.of(stop);
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }

    /**
     * Get all Stops whose name contains the substring (case-insensitive). For example, the parameter "Rice"
     * would return a List of Stops containing "Whitehead Rd @ Rice Hall"
     */
    public List<Stop> getStopsByName(String subString) throws SQLException {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM Stops WHERE StopName LIKE '" + subString + "'");
            List<Stop> stops = new ArrayList<>();
            while (rs.next()) {
                Stop stop = new Stop(rs.getInt("ID"), rs.getString("StopName"), rs.getDouble("Latitude"), rs.getDouble("Longitude"));
                stops.add(stop);
            }
            return stops;
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }

    /**
     * Add BusLines and their Routes to the database, including their routes. This method should only be called after
     * Stops are added to the database via addStops, since Routes depends on the StopIds already being
     * in the database. If any SQLExceptions occur, this method will rollback all changes since
     * the method was called. This could happen if, for example, a BusLine contains a Stop that is not in the database.
     */
    public void addBusLines(List<BusLine> busLines) throws SQLException {
        try {
            String insertRouteSQL = "INSERT INTO Routes(ID, StopID, BusLineID, RouteOrder) VALUES (?, ?, ?, ?)";
            String insertBusLineSQL = "INSERT INTO BusLines(ID, IsActive, LongName, ShortName) VALUES (?, ?, ?, ?)";

            PreparedStatement routeStmt = connection.prepareStatement(insertRouteSQL);
            PreparedStatement busLineStmt = connection.prepareStatement(insertBusLineSQL);
            for (BusLine busLine : busLines) {
                busLineStmt.setInt(1, busLine.getId());
                busLineStmt.setInt(2, busLine.isActive() ? 1 : 0);
                busLineStmt.setString(3, busLine.getLongName());
                busLineStmt.setString(4, busLine.getShortName());
                busLineStmt.addBatch();
            }
            busLineStmt.executeBatch();

            int routePrimKey = 1;
            for (BusLine busLine : busLines) {
                int routeOrder = 0;
                for (Stop stop : busLine.getRoute().getStops()) {
                    routeStmt.setInt(1, routePrimKey);
                    routeStmt.setInt(2, stop.getId());
                    routeStmt.setInt(3, busLine.getId());
                    routeStmt.setInt(4, routeOrder);
                    routeStmt.addBatch();
                    routeOrder++;
                    routePrimKey++;
                }
            }
            routeStmt.executeBatch();
        } catch (SQLException e) {
            rollback();
            throw e;
        }
    }

    /**
     * Return a list of all BusLines
     */
    public List<BusLine> getBusLines() throws SQLException{
        Statement stmtBus = connection.createStatement();

        ResultSet rsBus = stmtBus.executeQuery("SELECT * FROM BusLines");
        List<BusLine> busLines = new ArrayList<>();

        // Loop through BusLines
        while (rsBus.next()) {
            int busID = rsBus.getInt("ID");
            busLines.add(new BusLine(busID, rsBus.getBoolean("IsActive"), rsBus.getString("LongName"), rsBus.getString("ShortName"), getRouteFromBusID(busID)));
        }
        return busLines;
    }

    /**
     * Get a BusLine by its id number. Return Optional.empty() if no busLine is found
     */
    public Optional<BusLine> getBusLinesById(int busLineId) throws SQLException {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM BusLines WHERE ID = " + busLineId);
            if (!rs.next()) {
                return Optional.empty();
            }
            BusLine busLine = new BusLine(busLineId, rs.getBoolean("IsActive"), rs.getString("LongName"), rs.getString("ShortName"));
            return Optional.of(busLine);
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }

    /**
     * Get BusLine by its full long name (case-insensitive). Return Optional.empty() if no busLine is found.
     */
    public Optional<BusLine> getBusLineByLongName(String longName) throws SQLException {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM BusLines WHERE LongName = '" + longName + "'");
            if (!rs.next()) {
                return Optional.empty();
            }
            BusLine busLine = new BusLine(rs.getInt("ID"), rs.getBoolean("IsActive"), longName, rs.getString("ShortName"));
            return Optional.of(busLine);
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }

    /**
     * Get BusLine by its full short name (case-insensitive). Return Optional.empty() if no busLine is found.
     */
    public Optional<BusLine> getBusLineByShortName(String shortName) throws SQLException {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM BusLines WHERE ShortName = '" + shortName + "'");
            if (!rs.next()) {
                return Optional.empty();
            }
            BusLine busLine = new BusLine(rs.getInt("ID"), rs.getBoolean("IsActive"), rs.getString("LongName"), shortName);
            return Optional.of(busLine);
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }

    /**
     * Get all BusLines that visit a particular stop
     */
    public List<BusLine> getBusLinesByStop(Stop stop) throws SQLException {
        try {
            Statement stmtRoutes = connection.createStatement();
            Statement stmtBusLines = connection.createStatement();
            ResultSet rsRoute = stmtRoutes.executeQuery("SELECT * FROM Routes WHERE StopID = " + stop.getId());
            List<BusLine> busLines = new ArrayList<>();
            while (rsRoute.next()) {
                int busLineID = rsRoute.getInt("ID");
                ResultSet rsBus = stmtBusLines.executeQuery("SELECT * FROM BusLines WHERE BusLineID = " + busLineID);
                busLines.add(new BusLine(busLineID, rsBus.getBoolean("IsActive"), rsBus.getString("LongName"), rsBus.getString("ShortName")));
            }
            return busLines;
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }

    /**
     * Returns a BusLine's route as a List of stops *in-order*
     * @param busLine
     * @throws SQLException
     * @throws java.util.NoSuchElementException if busLine is not in the database
     */
    public Route getRouteForBusLine(BusLine busLine) throws SQLException {
        try {
            return getRouteFromBusID(busLine.getId());
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }

    /**
     * Removes all data from the tables, leaving the tables empty (but still existing!). As a hint, delete the
     * contents of Routes first in order to avoid violating foreign key constraints.
     */
    public void clearTables() throws SQLException {
        try {
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("DELETE FROM Routes");
            stmt.executeUpdate("DELETE FROM BusLines");
            stmt.executeUpdate("DELETE FROM Stops");
        } catch (SQLException e) {
            connection.rollback(); // Unsure whether to rollback if exception is caught or not
            throw e;
        }
    }

    // Helper method for creating a route for a busLine
    private Route getRouteFromBusID(int busLineId) throws SQLException {
        try {
            Statement stmtRoute = connection.createStatement();
            Statement stmtStop = connection.createStatement();
            ResultSet rsRoute = stmtRoute.executeQuery("SELECT * FROM Routes WHERE BusLineID = " + busLineId);
            Route route = new Route();
            // Loop through stops on Route
            while (rsRoute.next()) {
                int stopID = rsRoute.getInt("StopID");
                // Find the Stop in Stops that matches
                ResultSet rsStop = stmtStop.executeQuery("SELECT * FROM Stops WHERE ID = " + stopID);
                if (rsStop.next()) {
                    route.add(new Stop(rsStop.getInt("ID"), rsStop.getString("StopName"), rsStop.getDouble("Latitude"), rsStop.getDouble("Longitude")));
                } else {
                    throw new SQLException("No Stop matches StopID on route");
                }
            }
            return route;
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }
}
