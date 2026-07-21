package com.allocator.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentSucceededEvent extends BaseEvent {
    private String userId;
    private String email;
    private String name;
    private String planTier;
    private String planName;
    private String subscriptionId;
    private String stripeInvoiceId;
    private String invoiceNumber;
    private String amount;
    private String currency;
    private String paidAt;
    private String invoiceUrl;
    private String paymentMethodSummary;
}
