package com.app.shared.events;

import java.util.UUID;

public record TourCreatedEvent(
        String eventType,
        UUID tourId,
        String name,
        String city,
        double price
) {}
