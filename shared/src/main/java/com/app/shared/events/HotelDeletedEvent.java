package com.app.shared.events;

import java.util.UUID;

public record HotelDeletedEvent(
        String eventType,
        UUID hotelId
) {}
