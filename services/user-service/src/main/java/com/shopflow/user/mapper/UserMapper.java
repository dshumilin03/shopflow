package com.shopflow.user.mapper;

import com.shopflow.user.dto.UserRequest;
import com.shopflow.user.dto.UserResponse;
import com.shopflow.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    User toEntity(UserRequest userRequest);
    UserResponse toResponse(User user);
}
