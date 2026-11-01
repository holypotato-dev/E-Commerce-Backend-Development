package com.aadil.ecommerce.controllers;

import com.aadil.ecommerce.dtos.LoginRequestDTO;
import com.aadil.ecommerce.responses.UserLoginResponse;
import com.aadil.ecommerce.services.AuthService;
import com.aadil.ecommerce.dtos.UserRequestDTO;
import com.aadil.ecommerce.dtos.UserResponseDTO;
import com.aadil.ecommerce.responses.APIResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<APIResponse<UserResponseDTO>> register(@Valid @RequestBody UserRequestDTO dto) {

        UserResponseDTO userResponseDTO = authService.registerUser(dto);

        APIResponse<UserResponseDTO> apiResponse = APIResponse.<UserResponseDTO>builder()
                .success(true)
                .code(201)
                .message("User registered successfully")
                .data(userResponseDTO)
                .build();

        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<APIResponse<UserLoginResponse>> login(@Valid @RequestBody LoginRequestDTO dto) {
        UserLoginResponse token = authService.userLogin(dto);

        APIResponse<UserLoginResponse> apiResponse = APIResponse.<UserLoginResponse>builder()
                .success(true)
                .code(200)
                .message("Token Generated Successfully")
                .data(token)
                .build();

        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}
