package com.allocator.paymentservice.service;

import com.allocator.paymentservice.dto.AdminPlanDto;
import com.allocator.paymentservice.dto.CreatePlanRequest;
import com.allocator.paymentservice.dto.PlanDto;
import com.allocator.paymentservice.dto.UpdatePlanRequest;
import com.allocator.paymentservice.entity.Plan;
import com.allocator.paymentservice.enums.PlanTier;
import com.allocator.paymentservice.repository.PlanRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.PriceUpdateParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.ProductUpdateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanService {

    private final PlanRepository planRepository;

    public List<PlanDto> getActivePlans() {
        return planRepository.findByActiveTrue()
                .stream()
                .map(PlanDto::from)
                .toList();
    }

    public List<AdminPlanDto> getAllPlansForAdmin() {
        return planRepository.findAll()
                .stream()
                .map(AdminPlanDto::from)
                .toList();
    }

    @Transactional
    public AdminPlanDto createPlan(CreatePlanRequest request) throws StripeException {
        String stripeInterval = "YEARLY".equalsIgnoreCase(request.getInterval()) ? "year" : "month";

        // 1. Create Stripe Product
        ProductCreateParams productParams = ProductCreateParams.builder()
                .setName(request.getName())
                .build();
        Product stripeProduct = Product.create(productParams);
        log.info("Stripe product created: {}", stripeProduct.getId());

        // 2. Create Stripe Price linked to the product
        PriceCreateParams priceParams = PriceCreateParams.builder()
                .setProduct(stripeProduct.getId())
                .setUnitAmount(request.getAmountInCents())
                .setCurrency(request.getCurrency().toLowerCase())
                .setRecurring(PriceCreateParams.Recurring.builder()
                        .setInterval("year".equals(stripeInterval)
                                ? PriceCreateParams.Recurring.Interval.YEAR
                                : PriceCreateParams.Recurring.Interval.MONTH)
                        .build())
                .build();
        Price stripePrice = Price.create(priceParams);
        log.info("Stripe price created: {}", stripePrice.getId());

        // 3. Persist to local DB
        Plan plan = new Plan();
        plan.setName(request.getName());
        plan.setTier(request.getTier());
        plan.setStripeProductId(stripeProduct.getId());
        plan.setStripePriceId(stripePrice.getId());
        plan.setAmount(BigDecimal.valueOf(request.getAmountInCents()).divide(BigDecimal.valueOf(100)));
        plan.setCurrency(request.getCurrency().toUpperCase());
        plan.setInterval(stripeInterval);
        plan.setActive(true);

        Plan saved = planRepository.save(plan);
        log.info("Plan saved to DB: {}", saved.getId());

        return AdminPlanDto.from(saved);
    }

    public Plan getPlanOrThrow(UUID planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));
    }

    public Plan getPlanById(UUID planId) {
        return getPlanOrThrow(planId);
    }

    /**
     * Stripe Prices are immutable, so changing a plan's price creates a new Stripe Price on the
     * same Product and archives the old one. Existing subscribers already on the old price keep
     * paying that price until explicitly moved to the new one.
     */
    @Transactional
    public AdminPlanDto updatePlan(UUID planId, UpdatePlanRequest request) throws StripeException {
        Plan plan = getPlanOrThrow(planId);

        if (request.getName() != null && !request.getName().isBlank() && !request.getName().equals(plan.getName())) {
            Product.retrieve(plan.getStripeProductId())
                    .update(ProductUpdateParams.builder().setName(request.getName()).build());
            plan.setName(request.getName());
        }

        if (request.hasPriceChange()) {
            if (request.getAmountInCents() == null || request.getCurrency() == null || request.getInterval() == null) {
                throw new IllegalArgumentException("amountInCents, currency and interval must all be provided to change a plan's price");
            }

            String stripeInterval = "YEARLY".equalsIgnoreCase(request.getInterval()) ? "year" : "month";

            PriceCreateParams priceParams = PriceCreateParams.builder()
                    .setProduct(plan.getStripeProductId())
                    .setUnitAmount(request.getAmountInCents())
                    .setCurrency(request.getCurrency().toLowerCase())
                    .setRecurring(PriceCreateParams.Recurring.builder()
                            .setInterval("year".equals(stripeInterval)
                                    ? PriceCreateParams.Recurring.Interval.YEAR
                                    : PriceCreateParams.Recurring.Interval.MONTH)
                            .build())
                    .build();
            Price newPrice = Price.create(priceParams);
            log.info("New Stripe price created for plan {}: {}", plan.getId(), newPrice.getId());

            if (plan.getStripePriceId() != null) {
                Price.retrieve(plan.getStripePriceId())
                        .update(PriceUpdateParams.builder().setActive(false).build());
                log.info("Archived old Stripe price {} for plan {}", plan.getStripePriceId(), plan.getId());
            }

            plan.setStripePriceId(newPrice.getId());
            plan.setAmount(BigDecimal.valueOf(request.getAmountInCents()).divide(BigDecimal.valueOf(100)));
            plan.setCurrency(request.getCurrency().toUpperCase());
            plan.setInterval(stripeInterval);
        }

        Plan saved = planRepository.save(plan);
        log.info("Plan updated: {}", saved.getId());
        return AdminPlanDto.from(saved);
    }
}
