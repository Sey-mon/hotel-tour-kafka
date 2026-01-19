package com.app.consumer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbCheck {
    public static void main(String[] args) throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:booking.db");
             Statement st = conn.createStatement()) {

            System.out.println("Hotels:");
            try (ResultSet rs = st.executeQuery("SELECT hotel_id, name, city, price_per_night FROM hotels")) {
                while (rs.next()) {
                    System.out.printf("- %s | %s | %s | %.2f%n",
                            rs.getString(1), rs.getString(2), rs.getString(3), rs.getDouble(4));
                }
            }

            System.out.println("\nTours:");
            try (ResultSet rs = st.executeQuery("SELECT tour_id, name, city, price FROM tours")) {
                while (rs.next()) {
                    System.out.printf("- %s | %s | %s | %.2f%n",
                            rs.getString(1), rs.getString(2), rs.getString(3), rs.getDouble(4));
                }
            }

            System.out.println("\nBookings:");
            try (ResultSet rs = st.executeQuery("SELECT booking_id, customer_name, hotel_id, tour_id, nights, created_at FROM bookings")) {
                while (rs.next()) {
                    System.out.printf("- %s | %s | hotel=%s | tour=%s | nights=%d | %s%n",
                            rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getInt(5), rs.getString(6));
                }
            }
        }
    }
}
