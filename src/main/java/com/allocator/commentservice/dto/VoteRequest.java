package com.allocator.commentservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteRequest {

    /** +1 (upvote) or -1 (downvote). */
    @NotNull
    private Short value;
}
