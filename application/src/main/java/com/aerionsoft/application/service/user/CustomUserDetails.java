package com.aerionsoft.application.service.user;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
@Setter

public class  CustomUserDetails implements UserDetails {
    @Getter
    private final Long id;
    private final String username;
    private final String password;
    private final boolean isVerified;
    private final boolean isActive;
    private final Collection<? extends GrantedAuthority> authorities;
    @Getter
    private final String provider;
    @Getter
    private final Long impersonatedByAdminId;

    public CustomUserDetails(Long id, String username, String password, boolean isVerified, boolean isActive, Collection<? extends GrantedAuthority> authorities, String provider) {
        this(id, username, password, isVerified, isActive, authorities, provider, null);
    }

    public CustomUserDetails(Long id, String username, String password, boolean isVerified, boolean isActive, Collection<? extends GrantedAuthority> authorities, String provider, Long impersonatedByAdminId) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.isVerified = isVerified;
        this.isActive = isActive;
        this.authorities = authorities;
        this.provider = provider;
        this.impersonatedByAdminId = impersonatedByAdminId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isVerified && isActive;
    }
}

