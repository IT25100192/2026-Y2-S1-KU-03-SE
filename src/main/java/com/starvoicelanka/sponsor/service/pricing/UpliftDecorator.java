package com.starvoicelanka.sponsor.service.pricing;

/** Concrete Decorator - raises (or lowers, if negative) the wrapped price by a percentage. */
public class UpliftDecorator extends ContractPriceDecorator {

    private final double percent;

    public UpliftDecorator(ContractPrice wrapped, double percent) {
        super(wrapped);
        this.percent = percent;
    }

    @Override
    public String getDescription() {
        return wrapped.getDescription() + ", uplift " + percent + "%";
    }

    @Override
    public double getAmount() {
        return Math.round(wrapped.getAmount() * (1.0 + percent / 100.0));
    }
}
