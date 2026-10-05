package com.starvoicelanka.sponsor.service.pricing;

/**
 * DECORATOR PATTERN (Component interface) - Sponsor module.
 *
 * Same shape as the Coffee example in the lecture: a price that can be
 * wrapped by decorators, each adding its own change on top. Used when an
 * agreement is renewed, so the new price is built as
 * BasePrice -> UpliftDecorator -> (any further decorator).
 */
public interface ContractPrice {

    String getDescription();

    double getAmount();
}
