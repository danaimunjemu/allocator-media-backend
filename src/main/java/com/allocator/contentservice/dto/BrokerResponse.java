package com.allocator.contentservice.dto;

import com.allocator.contentservice.model.Broker;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrokerResponse {
    private UUID id;
    private String name;
    private String slug;
    private String tagline;
    private String logoUrl;
    private String website;
    private String overview;
    private Integer yearFounded;
    private String location;
    private String regulator;
    private List<Broker.Fact> facts;
    private List<String> platforms;
    private Map<String, String> fees;
    private List<Broker.FAQ> faqs;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
