package com.drobnyd.drobnyd.auth;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.drobnyd.drobnyd.auth.dto.AuthenticatedUserResponse;

public record SessionUser(
        Integer localId,
        String firebaseUid,
        String email,
        String displayName,
        String accountType,
        String role,
        Integer printingPointId
) {
    public List<GrantedAuthority> authorities() {
        Set<GrantedAuthority> uniqueAuthorities = new LinkedHashSet<>();
        uniqueAuthorities.add(new SimpleGrantedAuthority("ROLE_" + accountType));
        uniqueAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        return List.copyOf(uniqueAuthorities);
    }

    public AuthenticatedUserResponse toResponse() {
        return new AuthenticatedUserResponse(
                localId,
                firebaseUid,
                email,
                displayName,
                accountType,
                role,
                printingPointId,
                authorities().stream().map(GrantedAuthority::getAuthority).toList()
        );
    }
}

