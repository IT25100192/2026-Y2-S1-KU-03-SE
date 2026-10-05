package com.starvoicelanka.payment.service.gateway;

import com.starvoicelanka.common.exception.BadRequestException;
import com.starvoicelanka.payment.entity.PaymentMethod;

/**
 * FACTORY PATTERN - Payment module.
 *
 * Same idea as VehicleFactory in the lecture: the caller asks for a payment
 * method and gets back a PaymentGateway, without knowing which concrete class
 * was created. Adding a new method means one new class and one new case here.
 */
public class PaymentGatewayFactory {

    public PaymentGateway createGateway(PaymentMethod method) {
        if (method == null) {
            throw new BadRequestException("Choose a payment method");
        }
        switch (method) {
            case CARD:
                return new CardPayment();
            case EZCASH:
                return new EzCashPayment();
            case MCASH:
                return new MCashPayment();
            case BANK:
                return new BankTransferPayment();
            default:
                throw new BadRequestException("Unsupported payment method: " + method);
        }
    }
}
