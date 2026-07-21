package com.allocator.commentservice.dto;

import com.allocator.commentservice.model.CommentRemovalReason;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemoveCommentRequest {

    @NotNull
    private CommentRemovalReason reason;
}
