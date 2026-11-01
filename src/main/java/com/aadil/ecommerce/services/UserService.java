package com.aadil.ecommerce.services;

import com.aadil.ecommerce.entities.AdminInvitationEntity;
import com.aadil.ecommerce.entities.UserEntity;
import com.aadil.ecommerce.enums.AdminRequestStatus;
import com.aadil.ecommerce.enums.UserRole;
import com.aadil.ecommerce.exceptions.*;
import com.aadil.ecommerce.repositories.AdminRepository;
import com.aadil.ecommerce.repositories.UserRepository;
import com.aadil.ecommerce.security.InvitationCreations;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final InvitationCreations invitationCreations;
    private final AdminRepository adminRepository;

    @Transactional
    public String invitationAcceptance(String rawToken) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new InvalidCredentialException("Authentication required to accept invitation");
        }

        String authEmail = authentication.getName().trim().toLowerCase();
        String hashedToken = invitationCreations.invitationHash(rawToken);

        AdminInvitationEntity invitation = adminRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new InvitationNotFoundException("Invitation has NOT found"));

        if (invitation.getAdminRequestStatus() != AdminRequestStatus.PENDING) {
            throw new InvalidInvitationException("Invitation doesn't have a PENDING request");
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvitationExpiredException("Invitation has expired");
        }

        UserEntity user = invitation.getUser();
        if (user == null) {
            throw new UserNotFoundException("This User is NO longer exists");
        }

        // Validate that caller is the designated recipient of the invitation (prevents IDOR)
        if (!user.getEmail().equalsIgnoreCase(authEmail)) {
            throw new InvalidRequestException("You are not authorized to accept this invitation");
        }

        if (user.getRole() == UserRole.ADMIN) {
            throw new UserAlreadyAnAdminException("This User is already an ADMIN");
        }

        invitation.setAdminRequestStatus(AdminRequestStatus.ACCEPTED);
        user.setRole(UserRole.ADMIN);

        userRepository.save(user);
        adminRepository.save(invitation);

        return "User has changed from USER to ADMIN successfully";
    }
}
