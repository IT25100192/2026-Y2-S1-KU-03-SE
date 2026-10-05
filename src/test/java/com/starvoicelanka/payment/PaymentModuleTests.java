package com.starvoicelanka.payment;

import com.starvoicelanka.payment.dto.PaymentDtos;
import com.starvoicelanka.payment.entity.CreditAccount;
import com.starvoicelanka.payment.entity.PaymentMethod;
import com.starvoicelanka.payment.service.PaymentService;
import com.starvoicelanka.user.entity.User;
import com.starvoicelanka.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class PaymentModuleTests {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testCreditAccountAndPurchase() {
        User voter = userRepository.findByEmailIgnoreCase("voter2@starvoice.lk").orElseThrow();
        CreditAccount account = paymentService.getAccount(voter.getId());
        int initialBalance = account.getBalance();

        PaymentDtos.PurchaseResultDto purchaseResult = paymentService.purchaseBundle(
                voter.getId(),
                "STARTER",
                PaymentMethod.CARD,
                "4111111111111111"
        );

        assertNotNull(purchaseResult);
        assertEquals(initialBalance + 10, purchaseResult.balance());

        // Test receipt and PDF generation
        Long paymentId = purchaseResult.payment().getId();
        PaymentDtos.ReceiptDto receipt = paymentService.getReceipt(paymentId, voter);
        assertNotNull(receipt);
        assertEquals(purchaseResult.payment().getReceiptNo(), receipt.receiptNo());

        byte[] pdfBytes = paymentService.renderReceiptPdf(paymentId, voter);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }
}
