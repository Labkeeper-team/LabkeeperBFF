package io.labkeeper.web.frontend.internal.mapper;

import io.labkeeper.businesslogic.api.domain.LabkeeperUser;
import io.labkeeper.web.common.api.domain.UserWebPresentation;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {
    UserWebPresentation toUserWebDto(LabkeeperUser user);
}
