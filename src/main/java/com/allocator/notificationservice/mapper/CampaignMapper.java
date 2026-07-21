package com.allocator.notificationservice.mapper;

import com.allocator.notificationservice.dto.CampaignRequest;
import com.allocator.notificationservice.dto.CampaignResponse;
import com.allocator.notificationservice.model.Campaign;
import com.allocator.notificationservice.model.CampaignStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = { CampaignStatus.class })
public interface CampaignMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "sentAt", ignore = true)
    @Mapping(target = "status", expression = "java(CampaignStatus.DRAFT)")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "submittedBy", ignore = true)
    @Mapping(target = "submittedAt", ignore = true)
    @Mapping(target = "reviewedBy", ignore = true)
    @Mapping(target = "reviewedAt", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "scheduledAt", ignore = true)
    Campaign toEntity(CampaignRequest request);

    @Mapping(target = "items", ignore = true)
    CampaignResponse toResponse(Campaign campaign);
}
