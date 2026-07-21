package com.allocator.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ArticlePublishedEvent extends BaseEvent {
    private String articleId;
    private String title;
    private String summary;
    private java.util.List<String> topics;
    private String authorId;
}
