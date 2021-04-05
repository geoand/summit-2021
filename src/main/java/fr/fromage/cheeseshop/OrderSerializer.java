package fr.fromage.cheeseshop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.Arc;
import org.apache.kafka.common.serialization.Serializer;

public class OrderSerializer implements Serializer<Order> {

    @Override
    public byte[] serialize(String topic, Order order) {
        try {
            return Arc.container().instance(ObjectMapper.class).get().writeValueAsBytes(order);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
