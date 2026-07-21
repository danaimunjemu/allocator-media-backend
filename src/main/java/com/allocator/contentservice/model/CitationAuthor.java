package com.allocator.contentservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitationAuthor {
    private String firstName;
    private String lastName;
    private String orcid;
    @Builder.Default
    private String role = "AUTHOR";
}
