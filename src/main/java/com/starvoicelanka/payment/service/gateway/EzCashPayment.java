package com.starvoicelanka.payment.service.gateway;

import java.util.UUID;

/** Concrete Strategy 2 - eZ Cash mobile wallet. */
public class EzCashPayment implements PaymentGateway {

    @Override
    public GatewayResult pay(double amountLKR, String card) {
        return new GatewayResult(true, "EZCASH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(), null);
    }
}
