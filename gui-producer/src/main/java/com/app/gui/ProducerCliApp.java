package com.app.gui;

import com.app.shared.events.BookingCreatedEvent;
import com.app.shared.events.HotelCreatedEvent;
import com.app.shared.events.TourCreatedEvent;
import com.app.shared.json.JsonUtil;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.Instant;
import java.util.UUID;

public class ProducerCliApp {

    public static void main(String[] args) throws Exception {
        String bootstrap = "localhost:9092";

        try (Producer<String, String> producer = KafkaProducerFactory.create(bootstrap)) {

            // 1) Create hotel
            UUID hotelId = UUID.randomUUID();
            HotelCreatedEvent hotel = new HotelCreatedEvent(
                    "HotelCreated",
                    hotelId,
                    "Sunset Hotel",
                    "Manila",
                    2500.0
            );
            producer.send(new ProducerRecord<>("hotel.created", hotelId.toString(), JsonUtil.toJson(hotel))).get();
            System.out.println("Sent hotel.created: " + hotelId);

            // 2) Create tour
            UUID tourId = UUID.randomUUID();
            TourCreatedEvent tour = new TourCreatedEvent(
                    "TourCreated",
                    tourId,
                    "Intramuros Walk",
                    "Manila",
                    799.0
            );
            producer.send(new ProducerRecord<>("tour.created", tourId.toString(), JsonUtil.toJson(tour))).get();
            System.out.println("Sent tour.created: " + tourId);

            // 3) Create booking
            UUID bookingId = UUID.randomUUID();
            BookingCreatedEvent booking = new BookingCreatedEvent(
                    "BookingCreated",
                    bookingId,
                    "Clint",
                    hotelId,
                    tourId,
                    2,
                    Instant.now()
            );
            producer.send(new ProducerRecord<>("booking.created", bookingId.toString(), JsonUtil.toJson(booking))).get();
            System.out.println("Sent booking.created: " + bookingId);
        }

        System.out.println("Done.");
    }
}
