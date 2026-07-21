package com.allocator.authservice.mapper;

import com.allocator.authservice.dto.UserDto;
import com.allocator.authservice.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "name", expression = "java(user.getFirstName() + \" \" + user.getLastName())")
    @Mapping(target = "status", expression = "java(user.isEnabled() ? \"active\" : \"inactive\")")
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "lastLogin", ignore = true)
    UserDto toDto(User user);

    List<UserDto> toDtoList(List<User> users);
}
