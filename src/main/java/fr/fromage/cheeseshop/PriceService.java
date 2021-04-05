package fr.fromage.cheeseshop;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.inject.Singleton;

@Singleton
public class PriceService {

    private final BitcoinPrice bitcoinPrice;

    public PriceService(@RestClient BitcoinPrice bitcoinPrice) {
        this.bitcoinPrice = bitcoinPrice;
    }

    public Uni<Double> priceInBitcoin(Cheese type) {
        return bitcoinPrice.get("USD", type.getDollarPrice());
    }

}
