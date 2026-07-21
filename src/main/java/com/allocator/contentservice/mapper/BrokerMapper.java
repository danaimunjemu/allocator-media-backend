package com.allocator.contentservice.mapper;

import com.allocator.contentservice.dto.BrokerRequest;
import com.allocator.contentservice.dto.BrokerResponse;
import com.allocator.contentservice.model.Broker;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BrokerMapper {

    Broker toEntity(BrokerRequest request);

    BrokerResponse toResponse(Broker broker);

    List<BrokerResponse> toResponseList(List<Broker> brokers);

    void updateEntity(BrokerRequest request, @MappingTarget Broker broker);
}
