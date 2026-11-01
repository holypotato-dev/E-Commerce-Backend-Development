package com.aadil.ecommerce.services;

import com.aadil.ecommerce.enums.UserRole;
import com.aadil.ecommerce.dtos.AdminBootStrapResponseDTO;
import com.aadil.ecommerce.dtos.BootStrapRequestDTO;
import com.aadil.ecommerce.entities.UserEntity;
import com.aadil.ecommerce.exceptions.UserAlreadyExistsException;
import com.aadil.ecommerce.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BootStrapService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private AdminBootStrapResponseDTO mapToDTO(UserEntity user) {
        return new AdminBootStrapResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }

    @Transactional
    public AdminBootStrapResponseDTO bootstrapAdmin(BootStrapRequestDTO dto) {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            throw new UserAlreadyExistsException("ADMIN is already present");
        }

        String email = dto.getEmail().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new UserAlreadyExistsException("A user with this email already exists");
        }

        String name = dto.getName().trim();
        String hashedPassword = passwordEncoder.encode(dto.getPassword());

        UserEntity user = new UserEntity();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(hashedPassword);
        user.setRole(UserRole.ADMIN);

        UserEntity savedUser = userRepository.save(user);

        return mapToDTO(savedUser);
    }
}
