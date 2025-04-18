package io.labkeeper.web.frontend.internal.mapper;

import io.labkeeper.businesslogic.api.domain.LabkeeperUser;
import io.labkeeper.web.common.api.domain.UserWebPresentation;
import org.mapstruct.Mapper;

@Mapper
public abstract class UserMapper {
    public abstract UserWebPresentation toUserWebDto(LabkeeperUser user);
}
