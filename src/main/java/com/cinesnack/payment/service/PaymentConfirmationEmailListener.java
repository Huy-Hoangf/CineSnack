package com.cinesnack.payment.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PaymentConfirmationEmailListener {

    private final GmailEmailClient client;

    public PaymentConfirmationEmailListener(GmailEmailClient client) {
        this.client = client;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentConfirmed(PaymentConfirmed payment) {
        client.send(payment);
    }
}
