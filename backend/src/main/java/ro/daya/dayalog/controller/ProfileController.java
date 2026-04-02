package ro.daya.dayalog.controller;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.daya.dayalog.dto.auth.CurrentUserResponse;
import ro.daya.dayalog.dto.profile.ChangePasswordRequest;
import ro.daya.dayalog.dto.profile.ChangePasswordResponse;
import ro.daya.dayalog.security.CurrentUserPrincipal;
import ro.daya.dayalog.service.ProfileService;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return new CurrentUserResponse(
                principal.getId(),
                principal.getRole(),
                principal.getUsername(),
                principal.getStudioId(),
                principal.getForcePasswordChange()
        );
    }

    @PostMapping("/change-password")
    public ChangePasswordResponse changePassword(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        return profileService.changePassword(principal, request);
    }
}