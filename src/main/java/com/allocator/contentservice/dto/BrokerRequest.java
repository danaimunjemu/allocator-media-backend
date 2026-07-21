package com.allocator.contentservice.dto;

import com.allocator.contentservice.model.Broker;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrokerRequest {
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
}
