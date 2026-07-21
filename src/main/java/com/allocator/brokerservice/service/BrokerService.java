package com.allocator.brokerservice.service;

import com.allocator.brokerservice.dto.BrokerRequest;
import com.allocator.brokerservice.dto.BrokerResponse;
import com.allocator.brokerservice.entity.Broker;
import com.allocator.brokerservice.repository.BrokerRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service("brokerService")
@RequiredArgsConstructor
@Slf4j
public class BrokerService {

    private final BrokerRepository repository;

    @Transactional(readOnly = true)
    public List<BrokerResponse> findActive(UUID brandId, String country, String licenseType,
                                           BigDecimal minInvestment, String tradingPlatform) {
        Specification<Broker> spec = buildSpec(brandId, country, licenseType, minInvestment, tradingPlatform);
        return repository.findAll(spec).stream().map(BrokerResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public BrokerResponse findActiveById(UUID id) {
        return repository.findByIdAndActiveTrue(id)
                .map(BrokerResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Broker not found: " + id));
    }

    @Transactional
    public BrokerResponse create(BrokerRequest req) {
        Broker broker = new Broker();
        applyRequest(broker, req);
        broker.setActive(true);
        return BrokerResponse.from(repository.save(broker));
    }

    @Transactional
    public BrokerResponse update(UUID id, BrokerRequest req) {
        Broker broker = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NoSuchElementException("Broker not found: " + id));
        applyRequest(broker, req);
        return BrokerResponse.from(repository.save(broker));
    }

    /** Soft delete â€” sets active = false, record remains in DB. */
    @Transactional
    public void softDelete(UUID id) {
        Broker broker = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NoSuchElementException("Broker not found: " + id));
        broker.setActive(false);
        repository.save(broker);
        log.info("Soft-deleted broker {} ({})", broker.getName(), id);
    }

    private void applyRequest(Broker broker, BrokerRequest req) {
        broker.setBrandId(req.getBrandId());
        broker.setName(req.getName());
        broker.setLogoUrl(req.getLogoUrl());
        broker.setCountry(req.getCountry());
        broker.setLicenseType(req.getLicenseType());
        broker.setTradingPlatform(req.getTradingPlatform());
        broker.setMinimumInvestment(req.getMinimumInvestment());
        broker.setCurrency(req.getCurrency());
        broker.setWebsiteUrl(req.getWebsiteUrl());
        broker.setDescription(req.getDescription());
        if (req.getFeatured() != null) broker.setFeatured(req.getFeatured());
    }

    private Specification<Broker> buildSpec(UUID brandId, String country, String licenseType,
                                             BigDecimal minInvestment, String tradingPlatform) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));

            if (brandId != null) {
                predicates.add(cb.equal(root.get("brandId"), brandId));
            }
            if (country != null && !country.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("country")),
                        "%" + country.toLowerCase() + "%"));
            }
            if (licenseType != null && !licenseType.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("licenseType")),
                        "%" + licenseType.toLowerCase() + "%"));
            }
            if (tradingPlatform != null && !tradingPlatform.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("tradingPlatform")),
                        "%" + tradingPlatform.toLowerCase() + "%"));
            }
            if (minInvestment != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("minimumInvestment"), minInvestment));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

