package com.app.gui.hotel;

import com.app.shared.events.HotelCreatedEvent;
import com.app.shared.events.HotelDeletedEvent;
import com.app.shared.events.HotelUpdatedEvent;
import com.app.shared.json.JsonUtil;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.UUID;

public final class HotelKafkaProducer implements AutoCloseable {
    private final Producer<String, String> producer;

    public HotelKafkaProducer(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        this.producer = new KafkaProducer<>(props);
    }

    public void createHotel(UUID id, String name, String city, double pricePerNight) throws Exception {
        HotelCreatedEvent e = new HotelCreatedEvent("HotelCreated", id, name, city, pricePerNight);
        producer.send(new ProducerRecord<>("hotel.created", id.toString(), JsonUtil.toJson(e))).get();
    }

    public void updateHotel(UUID id, String name, String city, double pricePerNight) throws Exception {
        HotelUpdatedEvent e = new HotelUpdatedEvent("HotelUpdated", id, name, city, pricePerNight);
        producer.send(new ProducerRecord<>("hotel.updated", id.toString(), JsonUtil.toJson(e))).get();
    }

    public void deleteHotel(UUID id) throws Exception {
        HotelDeletedEvent e = new HotelDeletedEvent("HotelDeleted", id);
        producer.send(new ProducerRecord<>("hotel.deleted", id.toString(), JsonUtil.toJson(e))).get();
    }

    @Override
    public void close() {
        producer.close();
    }
}
