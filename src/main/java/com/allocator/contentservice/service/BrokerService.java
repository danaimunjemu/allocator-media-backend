package com.allocator.contentservice.service;

import com.allocator.contentservice.dto.BrokerRequest;
import com.allocator.contentservice.dto.BrokerResponse;
import com.allocator.contentservice.mapper.BrokerMapper;
import com.allocator.contentservice.model.Broker;
import com.allocator.contentservice.repository.BrokerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service("contentBrokerService")
@RequiredArgsConstructor
@Slf4j
public class BrokerService {

    private final BrokerRepository brokerRepository;
    private final BrokerMapper brokerMapper;

    @Transactional(readOnly = true)
    public List<BrokerResponse> getAllBrokers() {
        return brokerMapper.toResponseList(brokerRepository.findAll());
    }

    @Transactional(readOnly = true)
    public BrokerResponse getBrokerBySlug(String slug) {
        return brokerRepository.findBySlug(slug)
                .map(brokerMapper::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Broker not found with slug: " + slug));
    }

    @Transactional
    public BrokerResponse createBroker(BrokerRequest request) {
        Broker broker = brokerMapper.toEntity(request);
        return brokerMapper.toResponse(brokerRepository.save(broker));
    }

    @Transactional
    public BrokerResponse updateBroker(UUID id, BrokerRequest request) {
        Broker broker = brokerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Broker not found with id: " + id));
        brokerMapper.updateEntity(request, broker);
        return brokerMapper.toResponse(brokerRepository.save(broker));
    }

    @Transactional
    public void deleteBroker(UUID id) {
        brokerRepository.deleteById(id);
    }
}

