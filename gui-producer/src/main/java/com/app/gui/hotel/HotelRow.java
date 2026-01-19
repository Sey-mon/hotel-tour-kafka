package com.app.gui.hotel;

public record HotelRow(
        String hotelId,
        String name,
        String city,
        double pricePerNight
) {}
