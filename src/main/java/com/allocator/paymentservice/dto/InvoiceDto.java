package com.allocator.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDto {
    /** Stripe invoice ID (e.g. in_1OxABC...) */
    private String id;

    /** Human-readable invoice number (e.g. STSH-0001) */
    private String number;

    /** Amount in major currency units (e.g. 9.99) */
    private double amount;

    /** ISO currency code, lowercase (e.g. "usd") */
    private String currency;

    /** Invoice status: "paid", "open", "void", "uncollectible" */
    private String status;

    /** ISO-8601 timestamp of invoice creation */
    private String invoiceDate;

    /** ISO-8601 timestamp of due date (null for auto-collect) */
    private String dueDate;

    /** URL to the hosted Stripe invoice page */
    private String invoiceUrl;

    /** URL to the PDF version of the invoice */
    private String invoicePdf;

    /** Short description, e.g. plan name */
    private String description;
}
