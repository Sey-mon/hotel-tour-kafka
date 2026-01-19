package com.app.consumer;

import com.app.consumer.db.Db;
import com.app.shared.events.BookingCreatedEvent;
import com.app.shared.events.BookingDeletedEvent;
import com.app.shared.events.HotelCreatedEvent;
import com.app.shared.events.HotelDeletedEvent;
import com.app.shared.events.HotelUpdatedEvent;
import com.app.shared.events.TourCreatedEvent;
import com.app.shared.events.TourDeletedEvent;
import com.app.shared.events.TourUpdatedEvent;
import com.app.shared.json.JsonUtil;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class BookingConsumerApp {

    public static void main(String[] args) {
        String bootstrap = "localhost:9092";

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "hotel-tour-consumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        try (Connection conn = Db.connect()) {
            Db.init(conn);

            try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
                consumer.subscribe(List.of(
                        "hotel.created",
                        "hotel.updated",
                        "hotel.deleted",
                        "tour.created",
                        "tour.updated",
                        "tour.deleted",
                        "booking.created",
                        "booking.deleted"
                ));

                System.out.println("Consumer started. Listening...");

                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                    records.forEach(r -> {
                        String topic = r.topic();
                        String json = r.value();

                        try {
                            switch (topic) {
                                // -------------------- HOTELS --------------------
                                case "hotel.created" -> {
                                    HotelCreatedEvent e = JsonUtil.fromJson(json, HotelCreatedEvent.class);
                                    saveHotel(conn, e);
                                    System.out.println("Saved hotel: " + e.hotelId());
                                }
                                case "hotel.updated" -> {
                                    HotelUpdatedEvent e = JsonUtil.fromJson(json, HotelUpdatedEvent.class);
                                    // Reuse saveHotel (INSERT OR REPLACE) to update
                                    saveHotel(conn, new HotelCreatedEvent(
                                            "HotelCreated",
                                            e.hotelId(),
                                            e.name(),
                                            e.city(),
                                            e.pricePerNight()
                                    ));
                                    System.out.println("Updated hotel: " + e.hotelId());
                                }
                                case "hotel.deleted" -> {
                                    HotelDeletedEvent e = JsonUtil.fromJson(json, HotelDeletedEvent.class);
                                    deleteHotel(conn, e.hotelId().toString());
                                    System.out.println("Deleted hotel: " + e.hotelId());
                                }

                                // -------------------- TOURS --------------------
                                case "tour.created" -> {
                                    TourCreatedEvent e = JsonUtil.fromJson(json, TourCreatedEvent.class);
                                    saveTour(conn, e);
                                    System.out.println("Saved tour: " + e.tourId());
                                }
                                case "tour.updated" -> {
                                    TourUpdatedEvent e = JsonUtil.fromJson(json, TourUpdatedEvent.class);
                                    // Reuse saveTour (INSERT OR REPLACE) to update
                                    saveTour(conn, new TourCreatedEvent(
                                            "TourCreated",
                                            e.tourId(),
                                            e.name(),
                                            e.city(),
                                            e.price()
                                    ));
                                    System.out.println("Updated tour: " + e.tourId());
                                }
                                case "tour.deleted" -> {
                                    TourDeletedEvent e = JsonUtil.fromJson(json, TourDeletedEvent.class);
                                    deleteTour(conn, e.tourId().toString());
                                    System.out.println("Deleted tour: " + e.tourId());
                                }

                                // -------------------- BOOKINGS --------------------
                                case "booking.created" -> {
                                    BookingCreatedEvent e = JsonUtil.fromJson(json, BookingCreatedEvent.class);
                                    saveBooking(conn, e);
                                    System.out.println("Saved booking: " + e.bookingId());
                                }
                                case "booking.deleted" -> {
                                    BookingDeletedEvent e = JsonUtil.fromJson(json, BookingDeletedEvent.class);
                                    deleteBooking(conn, e.bookingId().toString());
                                    System.out.println("Deleted booking: " + e.bookingId());
                                }

                                default -> System.out.println("Unknown topic: " + topic);
                            }
                        } catch (Exception ex) {
                            System.out.println("Failed processing message on " + topic + ": " + ex.getMessage());
                        }
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -------------------- DB WRITES --------------------

    private static void saveHotel(Connection conn, HotelCreatedEvent e) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
            INSERT OR REPLACE INTO hotels(hotel_id, name, city, price_per_night)
            VALUES (?, ?, ?, ?)
        """)) {
            ps.setString(1, e.hotelId().toString());
            ps.setString(2, e.name());
            ps.setString(3, e.city());
            ps.setDouble(4, e.pricePerNight());
            ps.executeUpdate();
        }
    }

    private static void saveTour(Connection conn, TourCreatedEvent e) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
            INSERT OR REPLACE INTO tours(tour_id, name, city, price)
            VALUES (?, ?, ?, ?)
        """)) {
            ps.setString(1, e.tourId().toString());
            ps.setString(2, e.name());
            ps.setString(3, e.city());
            ps.setDouble(4, e.price());
            ps.executeUpdate();
        }
    }

    private static void saveBooking(Connection conn, BookingCreatedEvent e) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
            INSERT OR REPLACE INTO bookings(booking_id, customer_name, hotel_id, tour_id, nights, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """)) {
            ps.setString(1, e.bookingId().toString());
            ps.setString(2, e.customerName());
            ps.setString(3, e.hotelId().toString());
            ps.setString(4, e.tourId() == null ? null : e.tourId().toString());
            ps.setInt(5, e.nights());
            ps.setString(6, e.createdAt().toString());
            ps.executeUpdate();
        }
    }

    // -------------------- DB DELETES --------------------

    private static void deleteHotel(Connection conn, String hotelId) throws Exception {
        try (PreparedStatement ps =
                     conn.prepareStatement("DELETE FROM hotels WHERE hotel_id = ?")) {
            ps.setString(1, hotelId);
            ps.executeUpdate();
        }
    }

    private static void deleteTour(Connection conn, String tourId) throws Exception {
        try (PreparedStatement ps =
                     conn.prepareStatement("DELETE FROM tours WHERE tour_id = ?")) {
            ps.setString(1, tourId);
            ps.executeUpdate();
        }
    }

    private static void deleteBooking(Connection conn, String bookingId) throws Exception {
        try (PreparedStatement ps =
                     conn.prepareStatement("DELETE FROM bookings WHERE booking_id = ?")) {
            ps.setString(1, bookingId);
            ps.executeUpdate();
        }
    }
}
