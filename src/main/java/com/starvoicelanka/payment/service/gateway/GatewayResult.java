package com.starvoicelanka.payment.service.gateway;

/** What a payment strategy returns after trying to take the money. */
public record GatewayResult(boolean approved, String reference, String reason) {}
