package com.starvoicelanka.patterns;

import com.starvoicelanka.common.exception.ValidationException;
import com.starvoicelanka.common.validation.InputValidator;
import com.starvoicelanka.contestant.entity.RoundStatus;
import com.starvoicelanka.contestant.service.transition.RoundTransitionFactory;
import com.starvoicelanka.notification.entity.NotificationChannel;
import com.starvoicelanka.notification.service.provider.ConsoleProvider;
import com.starvoicelanka.notification.service.provider.InAppProvider;
import com.starvoicelanka.notification.service.provider.NotificationProviderFactory;
import com.starvoicelanka.notification.service.provider.SendGridEmailProvider;
import com.starvoicelanka.notification.service.provider.TwilioSmsProvider;
import com.starvoicelanka.payment.entity.PaymentMethod;
import com.starvoicelanka.payment.service.gateway.BankTransferPayment;
import com.starvoicelanka.payment.service.gateway.CardPayment;
import com.starvoicelanka.payment.service.gateway.GatewayResult;
import com.starvoicelanka.payment.service.gateway.PaymentGatewayFactory;
import com.starvoicelanka.sponsor.service.pricing.BasePrice;
import com.starvoicelanka.sponsor.service.pricing.ContractPrice;
import com.starvoicelanka.sponsor.service.pricing.UpliftDecorator;
import com.starvoicelanka.user.entity.Role;
import com.starvoicelanka.user.service.policy.PasswordPolicyFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Plain unit tests (no Spring, no database) for the Strategy / Factory / Decorator classes and the shared validator. */
class PatternsAndValidationTests {

    /* ---- Payment: Strategy + Factory ------------------------------------ */

    @Test
    void paymentFactoryReturnsTheRightStrategy() {
        PaymentGatewayFactory factory = new PaymentGatewayFactory();
        assertInstanceOf(CardPayment.class, factory.createGateway(PaymentMethod.CARD));
        assertInstanceOf(BankTransferPayment.class, factory.createGateway(PaymentMethod.BANK));
    }

    @Test
    void cardEndingInZerosIsDeclinedButOthersApprove() {
        PaymentGatewayFactory factory = new PaymentGatewayFactory();
        GatewayResult declined = factory.createGateway(PaymentMethod.CARD).pay(100, "4111111111110000");
        assertFalse(declined.approved());
        GatewayResult ok = factory.createGateway(PaymentMethod.EZCASH).pay(100, null);
        assertTrue(ok.approved());
        assertTrue(ok.reference().startsWith("EZCASH-"));
    }

    /* ---- Notification: Factory ------------------------------------------ */

    @Test
    void notificationFactoryPicksProviderByChannelAndName() {
        NotificationProviderFactory f = new NotificationProviderFactory();
        assertInstanceOf(InAppProvider.class, f.createProvider("sendgrid", NotificationChannel.IN_APP));
        assertInstanceOf(SendGridEmailProvider.class, f.createProvider("sendgrid", NotificationChannel.EMAIL));
        assertInstanceOf(TwilioSmsProvider.class, f.createProvider("twilio", NotificationChannel.SMS));
        assertInstanceOf(ConsoleProvider.class, f.createProvider("console", NotificationChannel.EMAIL));
    }

    /* ---- Contestant: Factory --------------------------------------------- */

    @Test
    void roundTransitionRulesMatchTheLifecycle() {
        RoundTransitionFactory f = new RoundTransitionFactory();
        assertTrue(f.createTransition(RoundStatus.DRAFT).canMoveTo(RoundStatus.OPEN));
        assertFalse(f.createTransition(RoundStatus.DRAFT).canMoveTo(RoundStatus.CLOSED));
        assertTrue(f.createTransition(RoundStatus.OPEN).canMoveTo(RoundStatus.CLOSED));
        assertTrue(f.createTransition(RoundStatus.CLOSED).canMoveTo(RoundStatus.RESULTS_PUBLISHED));
        assertFalse(f.createTransition(RoundStatus.RESULTS_PUBLISHED).canMoveTo(RoundStatus.OPEN));
    }

    /* ---- Sponsor: Decorator ---------------------------------------------- */

    @Test
    void upliftDecoratorWrapsTheBasePrice() {
        ContractPrice price = new BasePrice("Previous contract", 100000);
        price = new UpliftDecorator(price, 10);
        assertEquals(110000.0, price.getAmount(), 0.001);
        assertTrue(price.getDescription().contains("uplift"));
    }

    /* ---- User: Factory + Strategy ---------------------------------------- */

    @Test
    void passwordRulesDependOnRole() {
        PasswordPolicyFactory f = new PasswordPolicyFactory();
        assertDoesNotThrow(() -> f.createPolicy(Role.VOTER).validate("voter123"));
        assertThrows(ValidationException.class, () -> f.createPolicy(Role.VOTER).validate("short"));
        assertThrows(ValidationException.class, () -> f.createPolicy(Role.ADMIN).validate("voter123"));
        assertDoesNotThrow(() -> f.createPolicy(Role.ADMIN).validate("Admin12345"));
    }

    /* ---- Shared validator ------------------------------------------------- */

    @Test
    void requireTextRejectsBlankAndTooLong() {
        assertThrows(ValidationException.class, () -> InputValidator.requireText("   ", "Name", 2, 10));
        assertThrows(ValidationException.class, () -> InputValidator.requireText("a", "Name", 2, 10));
        assertThrows(ValidationException.class, () -> InputValidator.requireText("abcdefghijk", "Name", 2, 10));
        assertEquals("Kamal", InputValidator.requireText("  Kamal ", "Name", 2, 10));
    }

    @Test
    void emailMobileAndNicAreChecked() {
        assertThrows(ValidationException.class, () -> InputValidator.requireEmail("not-an-email", "Email"));
        assertEquals("a@b.lk", InputValidator.requireEmail(" A@B.lk ", "Email"));
        assertThrows(ValidationException.class, () -> InputValidator.requireMobile("12345", "Mobile"));
        assertEquals("0771234567", InputValidator.requireMobile("077 123 4567", "Mobile"));
        assertThrows(ValidationException.class, () -> InputValidator.optionalNic("123"));
        assertNull(InputValidator.optionalNic(""));
    }

    @Test
    void moneyAndRangeRejectNegativeAndZero() {
        assertThrows(ValidationException.class, () -> InputValidator.requireMoney(-5.0, "Price", false, 1000));
        assertThrows(ValidationException.class, () -> InputValidator.requireMoney(0.0, "Price", false, 1000));
        assertThrows(ValidationException.class, () -> InputValidator.requireMoney(null, "Price", false, 1000));
        assertEquals(50.0, InputValidator.requireMoney(50.0, "Price", false, 1000));
        assertThrows(ValidationException.class, () -> InputValidator.requireRange(0, "Credits", 1, 100));
    }

    @Test
    void cardNumberMustBeDigits() {
        assertThrows(ValidationException.class, () -> InputValidator.requireCardNumber("abcd"));
        assertThrows(ValidationException.class, () -> InputValidator.requireCardNumber(""));
        assertEquals("4111111111111111", InputValidator.requireCardNumber("4111 1111 1111 1111"));
    }
}
