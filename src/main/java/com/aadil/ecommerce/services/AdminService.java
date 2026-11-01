package com.aadil.ecommerce.services;

import com.aadil.ecommerce.dtos.InvitationDTO;
import com.aadil.ecommerce.dtos.UserResponseDTO;
import com.aadil.ecommerce.entities.AdminInvitationEntity;
import com.aadil.ecommerce.entities.UserEntity;
import com.aadil.ecommerce.enums.AdminRequestStatus;
import com.aadil.ecommerce.enums.SortDirections;
import com.aadil.ecommerce.enums.SortFields;
import com.aadil.ecommerce.enums.UserRole;
import com.aadil.ecommerce.exceptions.*;
import com.aadil.ecommerce.repositories.AdminRepository;
import com.aadil.ecommerce.repositories.UserRepository;
import com.aadil.ecommerce.responses.PaginatedResponse;
import com.aadil.ecommerce.security.InvitationCreations;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final InvitationCreations invitationCreations;

    private UserResponseDTO mapToUserDTO(UserEntity user) {
        return new UserResponseDTO(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    private UserEntity getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new InvalidCredentialException("Authentication is required");
        }
        String authEmail = authentication.getName().trim().toLowerCase();
        return userRepository.findByEmail(authEmail)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found: " + authEmail));
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<List<UserResponseDTO>> getAllUsers(
            String search, int pageNumber, int size, SortFields sortField, SortDirections sortDirection) {
        if (search != null && search.isBlank()) {
            search = search.trim();
        }

        if (pageNumber < 0) {
            throw new InvalidRequestException("PageNumber should NOT be below 0");
        }
        if (size < 1) {
            throw new InvalidRequestException("Size should NOT be below 1");
        }
        if (size > 100) {
            throw new InvalidRequestException("Size should NOT exceed 100");
        }
        if (sortField == null) {
            sortField = SortFields.ID;
        }
        if (sortDirection == null) {
            sortDirection = SortDirections.ASC;
        }

        Sort.Direction direction = sortDirection == SortDirections.ASC
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Sort sort = Sort.by(new Sort.Order(direction, sortField.getUserSortField()));
        Pageable pageable = PageRequest.of(pageNumber, size, sort);

        Page<UserEntity> user;

        if (search == null || search.isBlank()) {
            user = userRepository.findAll(pageable);
        } else {
            user = userRepository.findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(search, search, pageable);
        }

        List<UserResponseDTO> contents = user.getContent().stream().map(this::mapToUserDTO).toList();

        return new PaginatedResponse<>(
                contents,
                user.getNumber(),
                user.getSize(),
                user.getTotalElements(),
                user.getTotalPages(),
                user.isLast()
        );
    }

    @Transactional
    public void deleteUsersById(List<Long> ids, boolean confirm) {
        if (ids == null || ids.isEmpty()) {
            throw new InvalidRequestException("At least one ID is required");
        }

        if (ids.contains(null)) {
            throw new InvalidRequestException("Ids cannot be NULL");
        }

        Set<Long> duplicateIds = new HashSet<>();
        for (Long id : ids) {
            if (!duplicateIds.add(id)) {
                throw new InvalidRequestException("Duplicate ID found: " + id);
            }
        }

        UserEntity authUser = getAuthenticatedUser();
        if (duplicateIds.contains(authUser.getId())) {
            throw new InvalidRequestException("Authenticated ADMIN cannot delete their own account");
        }

        List<UserEntity> users = userRepository.findAllById(ids);
        if (users.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more user IDs do not exist");
        }
        if (!confirm) {
            throw new InvalidRequestException("Confirmation required to delete users");
        }
        userRepository.deleteAllById(ids);
    }

    @Transactional
    public void deleteUser(Long id, boolean confirm) {
        if (!confirm) {
            throw new InvalidRequestException("Type confirm to proceed");
        }

        UserEntity authUser = getAuthenticatedUser();
        if (authUser.getId().equals(id)) {
            throw new InvalidRequestException("Authenticated ADMIN cannot delete their own account");
        }

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User is NOT found: " + id));

        userRepository.deleteById(user.getId());
    }

    @Transactional
    public UserResponseDTO promoteToAdmin(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource NOT found: " + id));

        if (user.getRole() == UserRole.ADMIN) {
            throw new UserAlreadyAnAdminException("This user is already an ADMIN");
        }

        user.setRole(UserRole.ADMIN);
        UserEntity savedUser = userRepository.save(user);

        return mapToUserDTO(savedUser);
    }

    @Transactional
    public String invitationCreation(InvitationDTO dto) {
        String email = dto.getEmail().trim().toLowerCase();

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User email NOT found: " + email));

        if (user.getRole() == UserRole.ADMIN) {
            throw new UserAlreadyAnAdminException("This user is already an ADMIN");
        }

        if (adminRepository.existsByUserAndAdminRequestStatus(user, AdminRequestStatus.PENDING)) {
            throw new RequestPendingException("This user has already a PENDING request");
        }

        String rawToken = invitationCreations.generateInvitation();
        String hashedToken = invitationCreations.invitationHash(rawToken);

        AdminInvitationEntity invitation = new AdminInvitationEntity();
        invitation.setUser(user);
        invitation.setTokenHash(hashedToken);
        invitation.setAdminRequestStatus(AdminRequestStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now().plusHours(24));

        adminRepository.save(invitation);

        return rawToken;
    }

    @Transactional
    public UserResponseDTO adminDemotion(Long id) {
        UserEntity authUser = getAuthenticatedUser();

        UserEntity targetUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User is NOT found: " + id));

        if (authUser.getId().equals(targetUser.getId())) {
            throw new InvalidRequestException("Authenticated ADMIN cannot demote themselves");
        } else if (targetUser.getRole() == UserRole.USER) {
            throw new AlreadyAUserException("This user is already an USER");
        }

        targetUser.setRole(UserRole.USER);
        UserEntity savedUser = userRepository.save(targetUser);

        return mapToUserDTO(savedUser);
    }
}
