package com.starvoicelanka.sponsor.service.pricing;

/** Concrete Component - the starting price (the previous contract value). */
public class BasePrice implements ContractPrice {

    private final String label;
    private final double amount;

    public BasePrice(String label, double amount) {
        this.label = label;
        this.amount = amount;
    }

    @Override
    public String getDescription() {
        return label;
    }

    @Override
    public double getAmount() {
        return amount;
    }
}
