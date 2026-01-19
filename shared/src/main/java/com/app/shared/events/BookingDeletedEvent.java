package com.app.shared.events;

import java.util.UUID;

public record BookingDeletedEvent(
        String eventType,
        UUID bookingId
) {}
