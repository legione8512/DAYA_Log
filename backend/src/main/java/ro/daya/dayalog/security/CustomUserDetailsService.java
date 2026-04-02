package ro.daya.dayalog.security;

import java.util.UUID;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import ro.daya.dayalog.entity.AppUser;
import ro.daya.dayalog.repository.AppUserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public CustomUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new DisabledException("User account is inactive");
        }

        return toPrincipal(user);
    }

    public CurrentUserPrincipal loadPrincipalByUserId(UUID userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new DisabledException("User account is inactive");
        }

        return toPrincipal(user);
    }

    private CurrentUserPrincipal toPrincipal(AppUser user) {
        return new CurrentUserPrincipal(
                user.getId(),
                user.getStudio().getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.getForcePasswordChange(),
                user.getActive()
        );
    }
}