package com.starvoicelanka.payment.service.gateway;

/**
 * STRATEGY PATTERN (Strategy interface) - Payment module.
 *
 * Every payment method (card, eZ Cash, mCash, bank transfer) is one
 * interchangeable way to do the same job: take the money for a bundle.
 * PaymentService (the Context) only talks to this interface, so it never
 * needs an if-else or switch on the payment method.
 */
public interface PaymentGateway {

    /** Take {@code amountLKR}. {@code card} is only used by the card strategy. */
    GatewayResult pay(double amountLKR, String card);
}
