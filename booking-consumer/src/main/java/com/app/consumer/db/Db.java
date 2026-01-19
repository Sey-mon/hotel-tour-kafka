package com.app.consumer.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public final class Db {
    private Db() {}

    public static Connection connect() {
        try {
            // creates file booking.db in booking-consumer folder (working directory)
            return DriverManager.getConnection("jdbc:sqlite:booking.db");
        } catch (Exception e) {
            throw new RuntimeException("DB connect failed", e);
        }
    }

    public static void init(Connection conn) {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS hotels (
                    hotel_id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    city TEXT NOT NULL,
                    price_per_night REAL NOT NULL
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS tours (
                    tour_id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    city TEXT NOT NULL,
                    price REAL NOT NULL
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS bookings (
                    booking_id TEXT PRIMARY KEY,
                    customer_name TEXT NOT NULL,
                    hotel_id TEXT NOT NULL,
                    tour_id TEXT,
                    nights INTEGER NOT NULL,
                    created_at TEXT NOT NULL
                )
            """);
        } catch (Exception e) {
            throw new RuntimeException("DB init failed", e);
        }
    }
}
