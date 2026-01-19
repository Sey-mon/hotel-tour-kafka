package com.app.shared.events;

import java.util.UUID;

public record TourUpdatedEvent(
        String eventType,
        UUID tourId,
        String name,
        String city,
        double price
) {}
