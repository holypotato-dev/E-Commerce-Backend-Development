package com.aadil.ecommerce.services;

import com.aadil.ecommerce.dtos.LoginRequestDTO;
import com.aadil.ecommerce.dtos.UserRequestDTO;
import com.aadil.ecommerce.dtos.UserResponseDTO;
import com.aadil.ecommerce.entities.UserEntity;
import com.aadil.ecommerce.enums.UserRole;
import com.aadil.ecommerce.exceptions.InvalidCredentialException;
import com.aadil.ecommerce.exceptions.UserAlreadyExistsException;
import com.aadil.ecommerce.repositories.UserRepository;
import com.aadil.ecommerce.responses.UserLoginResponse;
import com.aadil.ecommerce.security.JwtUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtUtility jwtUtility;
    private final AuthenticationManager authenticationManager;

    private UserResponseDTO mapToDTO(UserEntity user) {
        return new UserResponseDTO(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    @Transactional
    public UserResponseDTO registerUser(UserRequestDTO dto) {
        String email = dto.getEmail().trim().toLowerCase();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new UserAlreadyExistsException("USER_ALREADY_REGISTERED");
        }

        String hashedPassword = passwordEncoder.encode(dto.getPassword());

        UserEntity user = new UserEntity();
        user.setName(dto.getName().trim());
        user.setEmail(email);
        user.setPassword(hashedPassword);
        user.setRole(UserRole.USER);

        UserEntity savedUser = userRepository.save(user);

        return mapToDTO(savedUser);
    }

    @Transactional(readOnly = true)
    public UserLoginResponse userLogin(LoginRequestDTO dto) {
        String email = dto.getEmail().trim().toLowerCase();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, dto.getPassword())
            );

            // Reuses the authenticated UserDetails principal without an extra database query
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtility.generateToken(userDetails);

            return new UserLoginResponse(token);
        } catch (AuthenticationException e) {
            throw new InvalidCredentialException("Invalid Credentials, Check your Email OR password");
        }
    }
}
