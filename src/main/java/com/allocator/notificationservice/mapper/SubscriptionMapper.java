package com.allocator.notificationservice.mapper;

import com.allocator.notificationservice.dto.SubscriptionRequest;
import com.allocator.notificationservice.model.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "mailchimpMemberId", ignore = true)
    Subscription toEntity(SubscriptionRequest request);
}
