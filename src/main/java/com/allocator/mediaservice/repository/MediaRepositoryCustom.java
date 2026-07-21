package com.allocator.mediaservice.repository;

import com.allocator.mediaservice.dto.MediaFilter;
import com.allocator.mediaservice.model.Media;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MediaRepositoryCustom {

    Page<Media> findAll(MediaFilter filter, Pageable pageable);
}
