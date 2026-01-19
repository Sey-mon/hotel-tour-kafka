package com.app.gui;

public record BookingRow(
        String bookingId,
        String customerName,
        String hotelName,
        String tourName,
        int nights,
        String createdAt
) {}
