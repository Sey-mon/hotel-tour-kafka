package com.app.gui;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public final class DbReader {
    private DbReader() {}

    // When you run from project root, this path is correct:
    // hotel-tour-kafka/booking-consumer/booking.db
    private static String dbUrl() {
        String wd = System.getProperty("user.dir");

        java.nio.file.Path p1 = java.nio.file.Paths.get(wd, "booking-consumer", "booking.db");        // if run from root
        java.nio.file.Path p2 = java.nio.file.Paths.get(wd).getParent().resolve("booking-consumer").resolve("booking.db"); // if run from gui-producer

        java.nio.file.Path dbPath = java.nio.file.Files.exists(p1) ? p1 : p2;

        return "jdbc:sqlite:" + dbPath.toAbsolutePath();
    }


    public static List<BookingRow> loadBookingView() {
        String sql = """
            SELECT
                b.booking_id,
                b.customer_name,
                h.name AS hotel_name,
                COALESCE(t.name, '') AS tour_name,
                b.nights,
                b.created_at
            FROM bookings b
            JOIN hotels h ON b.hotel_id = h.hotel_id
            LEFT JOIN tours t ON b.tour_id = t.tour_id
            ORDER BY b.created_at DESC
        """;

        List<BookingRow> out = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(dbUrl());
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                out.add(new BookingRow(
                        rs.getString("booking_id"),
                        rs.getString("customer_name"),
                        rs.getString("hotel_name"),
                        rs.getString("tour_name"),
                        rs.getInt("nights"),
                        rs.getString("created_at")
                ));
            }

            return out;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read DB. Make sure booking-consumer/booking.db exists.", e);
        }
    }
}
