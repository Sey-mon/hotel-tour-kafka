package com.app.shared.events;

import java.util.UUID;

public record HotelUpdatedEvent(
        String eventType,
        UUID hotelId,
        String name,
        String city,
        double pricePerNight
) {}
