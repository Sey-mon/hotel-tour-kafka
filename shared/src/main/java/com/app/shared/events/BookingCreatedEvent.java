package com.app.shared.events;

import java.time.Instant;
import java.util.UUID;

public record BookingCreatedEvent(
        String eventType,
        UUID bookingId,
        String customerName,
        UUID hotelId,
        UUID tourId, // nullable
        int nights,
        Instant createdAt
) {}
