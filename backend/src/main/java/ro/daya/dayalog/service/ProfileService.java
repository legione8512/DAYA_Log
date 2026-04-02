package ro.daya.dayalog.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.daya.dayalog.dto.profile.ChangePasswordRequest;
import ro.daya.dayalog.dto.profile.ChangePasswordResponse;
import ro.daya.dayalog.entity.AppUser;
import ro.daya.dayalog.exception.BadRequestException;
import ro.daya.dayalog.exception.NotFoundException;
import ro.daya.dayalog.repository.AppUserRepository;
import ro.daya.dayalog.security.CurrentUserPrincipal;

@Service
public class ProfileService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(AppUserRepository appUserRepository,
                          PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ChangePasswordResponse changePassword(CurrentUserPrincipal principal,
                                                 ChangePasswordRequest request) {
        AppUser user = appUserRepository.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Utilizatorul nu a fost găsit."));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Parola curentă este incorectă.");
        }

        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new BadRequestException(
                    "PROFILE_CHANGE_PASSWORD_CONFIRM_MISMATCH",
                    "Confirmarea parolei noi nu corespunde."
            );
        }

        if (request.currentPassword().equals(request.newPassword())) {
            throw new BadRequestException(
                    "PROFILE_CHANGE_PASSWORD_SAME_AS_CURRENT",
                    "Noua parolă trebuie să fie diferită de parola curentă."
            );
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setForcePasswordChange(false);

        return new ChangePasswordResponse("Parola a fost schimbată cu succes.");
    }
}