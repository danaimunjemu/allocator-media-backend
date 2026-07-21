package com.allocator.paymentservice.service;

import com.allocator.paymentservice.dto.InvoiceDto;
import com.allocator.paymentservice.entity.Subscriber;
import com.allocator.paymentservice.repository.SubscriberRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.param.InvoiceListParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final SubscriberRepository subscriptionRepository;

    /**
     * Fetches up to 24 Stripe invoices for the given user.
     * Returns an empty list for FREE-tier users (no Stripe customer).
     */
    public List<InvoiceDto> getInvoicesForUser(UUID userId) throws StripeException {
        Subscriber sub = subscriptionRepository.findByUserId(userId).orElse(null);

        if (sub == null || sub.getStripeCustomerId() == null) {
            log.info("No Stripe customer found for userId: {} — returning empty invoice list", userId);
            return Collections.emptyList();
        }

        InvoiceListParams params = InvoiceListParams.builder()
                .setCustomer(sub.getStripeCustomerId())
                .setLimit(24L)
                .build();

        return Invoice.list(params).getData().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private InvoiceDto toDto(Invoice invoice) {
        return InvoiceDto.builder()
                .id(invoice.getId())
                .number(invoice.getNumber())
                .amount(invoice.getAmountPaid() / 100.0)   // Stripe amounts are in cents
                .currency(invoice.getCurrency())
                .status(invoice.getStatus())
                .invoiceDate(invoice.getCreated() != null
                        ? Instant.ofEpochSecond(invoice.getCreated()).toString()
                        : null)
                .dueDate(invoice.getDueDate() != null
                        ? Instant.ofEpochSecond(invoice.getDueDate()).toString()
                        : null)
                .invoiceUrl(invoice.getHostedInvoiceUrl())
                .invoicePdf(invoice.getInvoicePdf())
                .description(invoice.getDescription() != null
                        ? invoice.getDescription()
                        : (invoice.getLines() != null && !invoice.getLines().getData().isEmpty()
                                ? invoice.getLines().getData().get(0).getDescription()
                                : null))
                .build();
    }
}
