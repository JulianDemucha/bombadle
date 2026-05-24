package com.bombadle.config;

import com.bombadle.entity.Player;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class PlayerPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public PlayerPrincipal(Player player) {
        this.id = player.getId();
        this.email = player.getEmail();
        this.password = player.getPassword();
        this.authorities = List.of(new SimpleGrantedAuthority(player.getRole().toString()));
    }

    public Long getPlayerId() {
        return id;
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
        return email; // email
    }
}