package ro.daya.dayalog.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import ro.daya.dayalog.entity.enums.UserRole;

public class CurrentUserPrincipal implements UserDetails {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final UUID id;
    private final UUID studioId;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private final Boolean forcePasswordChange;
    private final Boolean active;

    public CurrentUserPrincipal(UUID id,
                                UUID studioId,
                                String email,
                                String passwordHash,
                                UserRole role,
                                Boolean forcePasswordChange,
                                Boolean active) {
        this.id = id;
        this.studioId = studioId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.forcePasswordChange = forcePasswordChange;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudioId() {
        return studioId;
    }

    public UserRole getRole() {
        return role;
    }

    public Boolean getForcePasswordChange() {
        return forcePasswordChange;
    }

    public Boolean getActive() {
        return active;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(active);
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
}