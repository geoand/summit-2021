package fr.fromage.cheeseshop;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.Record;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import javax.inject.Singleton;
import java.time.LocalDateTime;

@Singleton
public class OrderService {

    private final Emitter<Record<Long, Order>> kafkaProducer;
    private final PriceService priceService;

    public OrderService(@Channel("cheese-orders") Emitter<Record<Long, Order>> kafkaProducer, PriceService priceService) {
        this.kafkaProducer = kafkaProducer;
        this.priceService = priceService;
    }

    public Uni<Order> order(CreateOrderRequest createOrderRequest) {
        Long customerId = createOrderRequest.getCustomerId();
        Uni<Customer> customer = Customer.findById(customerId);
        return customer.onItem().ifNull().failWith(new Exceptions.NoCustomerFound(customerId))
                .onItem().ifNotNull().transformToUni(c -> toOrder(createOrderRequest, c))
                .onItem()
                .call(o -> Panache.withTransaction(() -> Order.persist(o)))
                .call(o -> Uni.createFrom().completionStage(kafkaProducer.send(Record.of(o.id, o))));
    }

    private Uni<Order> toOrder(CreateOrderRequest createOrderRequest, Customer customer) {
        return priceService.priceInBitcoin(createOrderRequest.getType()).onItem().transform(p -> {
            Order order = new Order();
            order.customer = customer;
            order.type = createOrderRequest.getType();
            order.count = createOrderRequest.getCount();
            order.timestamp = LocalDateTime.now();
            order.status = Order.Status.Submitted;
            order.princeInBitcoins = p * createOrderRequest.getCount();
            return order;
        });
    }

    public Uni<Order> cancel(Long orderId) {
        Uni<Order> order = Order.findById(orderId);
        return order.onItem().ifNull().failWith(new Exceptions.NoOrderFound(orderId))
             .onItem().ifNotNull().call(o -> Panache.withTransaction(() -> {
            o.status = Order.Status.Canceled;
            return Order.persist(o);
        }));
    }
}
