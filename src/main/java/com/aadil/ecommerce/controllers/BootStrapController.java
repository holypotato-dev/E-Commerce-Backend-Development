package com.aadil.ecommerce.controllers;

import com.aadil.ecommerce.dtos.AdminBootStrapResponseDTO;
import com.aadil.ecommerce.dtos.BootStrapRequestDTO;
import com.aadil.ecommerce.responses.APIResponse;
import com.aadil.ecommerce.services.BootStrapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bootstrap")
@Validated
public class BootStrapController {
    private final BootStrapService bootStrapService;

    @PostMapping("/admin")
    public ResponseEntity<APIResponse<AdminBootStrapResponseDTO>> createAdmin(@Valid @RequestBody BootStrapRequestDTO dto) {

        AdminBootStrapResponseDTO admin = bootStrapService.bootstrapAdmin(dto);

        APIResponse<AdminBootStrapResponseDTO> apiResponse = APIResponse.<AdminBootStrapResponseDTO>builder()
                .success(true)
                .code(201)
                .message("Initial ADMIN created successfully")
                .data(admin)
                .build();

        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }
}
