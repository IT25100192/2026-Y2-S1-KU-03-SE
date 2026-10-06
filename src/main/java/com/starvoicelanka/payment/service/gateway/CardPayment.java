package com.starvoicelanka.payment.service.gateway;

import java.util.UUID;

/** Concrete Strategy 1 - credit / debit card through the card gateway. */
public class CardPayment implements PaymentGateway {

    @Override
    public GatewayResult pay(double amountLKR, String card) {
        // Test rule kept from the original code: a card ending 0000 is declined
        if (card != null && card.endsWith("0000")) {
            return new GatewayResult(false, "DECLINED-" + System.currentTimeMillis(), "Card declined by issuer");
        }
        return new GatewayResult(true, "CARD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(), null);
    }
}
