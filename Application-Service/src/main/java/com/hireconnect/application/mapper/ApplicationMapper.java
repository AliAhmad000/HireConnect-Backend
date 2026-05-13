package com.hireconnect.application.mapper;

import com.hireconnect.application.dto.response.ApplicationResponse;
import com.hireconnect.application.entity.Application;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    ApplicationResponse toResponse(Application application);
}
