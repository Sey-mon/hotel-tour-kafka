package com.app.gui.hotel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class HotelDbReader {
    private HotelDbReader() {}

    private static Path getDbPath() {
        String wd = System.getProperty("user.dir");
        Path p1 = Paths.get(wd, "booking-consumer", "booking.db"); // running from root
        Path p2 = Paths.get(wd).getParent().resolve("booking-consumer").resolve("booking.db"); // running from gui-producer
        return Files.exists(p1) ? p1 : p2;
    }

    private static String dbUrl() {
        return "jdbc:sqlite:" + getDbPath().toAbsolutePath();
    }

    private static void ensureTablesExist(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS hotels (
                    hotel_id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    city TEXT NOT NULL,
                    price_per_night REAL NOT NULL
                )
            """);
        }
    }

    public static List<HotelRow> loadHotels() {
        String sql = """
            SELECT hotel_id, name, city, price_per_night
            FROM hotels
            ORDER BY name ASC
        """;

        List<HotelRow> out = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(dbUrl())) {
            ensureTablesExist(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new HotelRow(
                            rs.getString("hotel_id"),
                            rs.getString("name"),
                            rs.getString("city"),
                            rs.getDouble("price_per_night")
                    ));
                }
            }
            return out;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read hotels from DB at: " + dbUrl() + " - " + e.getMessage(), e);
        }
    }
}
