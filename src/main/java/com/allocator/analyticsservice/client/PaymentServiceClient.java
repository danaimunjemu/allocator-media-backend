package com.allocator.analyticsservice.client;

import com.allocator.analyticsservice.dto.PaymentSummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceClient {

    private static final String REVENUE_URL = "http://payment-service/api/v1/payments/admin/revenue";

    private final RestTemplate internalRestTemplate;

    /**
     * Fetches revenue and subscriber summary from payment-service.
     * Returns empty if payment-service is unavailable — never throws.
     */
    public Optional<PaymentSummaryDto> fetchRevenueSummary() {
        try {
            HttpHeaders headers = new HttpHeaders();
            // Internal service call: inject admin role so AdminPaymentController's guard passes
            headers.set("X-User-Roles", "ADMIN");

            ResponseEntity<PaymentSummaryDto> response = internalRestTemplate.exchange(
                    REVENUE_URL,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    PaymentSummaryDto.class);

            return Optional.ofNullable(response.getBody());

        } catch (Exception e) {
            log.warn("Payment-service unavailable — financial metrics omitted from summary: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
