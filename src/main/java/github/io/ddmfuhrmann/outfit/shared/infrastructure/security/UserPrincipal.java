package github.io.ddmfuhrmann.outfit.shared.infrastructure.security;

import github.io.ddmfuhrmann.outfit.shared.domain.model.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapter that wraps the domain {@link User} entity as a Spring Security {@link UserDetails}.
 * Storing the domain User here avoids a second database lookup after authentication succeeds.
 */
public record UserPrincipal(User user) implements UserDetails {

    @Override
    public Collection<SimpleGrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getLogin();
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}
