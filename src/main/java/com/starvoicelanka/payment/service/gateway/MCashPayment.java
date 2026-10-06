package com.starvoicelanka.payment.service.gateway;

import java.util.UUID;

/** Concrete Strategy 3 - mCash mobile wallet. */
public class MCashPayment implements PaymentGateway {

    @Override
    public GatewayResult pay(double amountLKR, String card) {
        return new GatewayResult(true, "MCASH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(), null);
    }
}
