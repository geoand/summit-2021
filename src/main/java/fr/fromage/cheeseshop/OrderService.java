package fr.fromage.cheeseshop;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.Record;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.inject.Singleton;
import java.time.LocalDateTime;
import java.util.UUID;

@Singleton
public class OrderService {

    private final MutinyEmitter<Record<UUID, Order>> kafkaProducer;
    private final BitcoinPrice bitcoinPrice;

    public OrderService(@Channel("cheese-orders") MutinyEmitter<Record<UUID, Order>> kafkaProducer, @RestClient BitcoinPrice bitcoinPrice) {
        this.kafkaProducer = kafkaProducer;
        this.bitcoinPrice = bitcoinPrice;
    }

    public Uni<Order> order(CreateOrderRequest createOrderRequest) {
        Long customerId = createOrderRequest.getCustomerId();
        Uni<Customer> customer = Customer.findById(customerId);
        return customer.onItem().ifNull().failWith(new Exceptions.NoCustomerFound(customerId))
                .onItem().ifNotNull().transformToUni(c -> toOrder(createOrderRequest, c))
                .onItem()
                .call(o -> Panache.withTransaction(o::persist))
                .call(o -> kafkaProducer.send(Record.of(o.id, o)));
    }

    private Uni<Order> toOrder(CreateOrderRequest createOrderRequest, Customer customer) {
        return bitcoinPrice.get("USD", createOrderRequest.getType().getDollarPrice()).onItem().transform(p -> {
            Order order = new Order();
            order.id = UUID.randomUUID();
            order.customer = customer;
            order.type = createOrderRequest.getType();
            order.count = createOrderRequest.getCount();
            order.timestamp = LocalDateTime.now();
            order.status = Order.Status.Submitted;
            order.princeInBitcoins = p * createOrderRequest.getCount();
            return order;
        });
    }

    public Uni<Order> cancel(UUID orderId) {
        Uni<Order> order = Order.findById(orderId);
        return order.onItem().ifNull().failWith(new Exceptions.NoOrderFound(orderId))
             .onItem().ifNotNull().call(o -> Panache.withTransaction(() -> {
            o.status = Order.Status.Canceled;
            return Order.persist(o);
        }));
    }
}
