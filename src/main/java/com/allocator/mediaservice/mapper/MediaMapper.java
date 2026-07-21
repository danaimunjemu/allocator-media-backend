package com.allocator.mediaservice.mapper;

import com.allocator.mediaservice.dto.MediaResponse;
import com.allocator.mediaservice.model.Media;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface MediaMapper {

    MediaMapper INSTANCE = Mappers.getMapper(MediaMapper.class);

    MediaResponse toResponse(Media media);

    @Mapping(target = "id", ignore = true)
    Media toEntityForUpdate(MediaResponse response);
}
