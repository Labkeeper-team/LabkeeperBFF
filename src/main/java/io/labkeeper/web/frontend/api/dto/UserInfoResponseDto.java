package io.labkeeper.web.frontend.api.dto;

public record UserInfoResponseDto(
        String email,
        long id,
        boolean isAuthenticated
) {}
