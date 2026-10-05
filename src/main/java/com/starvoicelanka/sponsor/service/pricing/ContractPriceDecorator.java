package com.starvoicelanka.sponsor.service.pricing;

/** Abstract Decorator - wraps another ContractPrice and passes everything through by default. */
public abstract class ContractPriceDecorator implements ContractPrice {

    protected final ContractPrice wrapped;

    protected ContractPriceDecorator(ContractPrice wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public String getDescription() {
        return wrapped.getDescription();
    }

    @Override
    public double getAmount() {
        return wrapped.getAmount();
    }
}
