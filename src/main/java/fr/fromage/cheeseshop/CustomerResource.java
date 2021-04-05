package fr.fromage.cheeseshop;

import io.smallrye.mutiny.Uni;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.List;

@Path("customer")
public class CustomerResource {

    @GET
    public Uni<List<Customer>> findAll() {
        return Customer.listAll();
    }
}
