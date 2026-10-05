package com.starvoicelanka.payment.service.gateway;

import java.util.UUID;

/** Concrete Strategy 4 - online bank transfer. */
public class BankTransferPayment implements PaymentGateway {

    @Override
    public GatewayResult pay(double amountLKR, String card) {
        return new GatewayResult(true, "BANK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(), null);
    }
}
