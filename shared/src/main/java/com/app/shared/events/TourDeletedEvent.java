package com.app.shared.events;

import java.util.UUID;

public record TourDeletedEvent(
        String eventType,
        UUID tourId
) {}
