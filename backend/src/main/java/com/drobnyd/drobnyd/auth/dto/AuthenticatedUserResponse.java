package com.drobnyd.drobnyd.auth.dto;

import java.util.List;

public record AuthenticatedUserResponse(
        Integer localId,
        String firebaseUid,
        String email,
        String displayName,
        String accountType,
        String role,
        Integer printingPointId,
        List<String> authorities
) {
}

