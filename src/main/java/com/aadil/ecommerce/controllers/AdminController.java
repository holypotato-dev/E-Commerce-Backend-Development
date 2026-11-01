package com.aadil.ecommerce.controllers;

import com.aadil.ecommerce.dtos.InvitationDTO;
import com.aadil.ecommerce.dtos.UserResponseDTO;
import com.aadil.ecommerce.enums.SortDirections;
import com.aadil.ecommerce.enums.SortFields;
import com.aadil.ecommerce.responses.APIResponse;
import com.aadil.ecommerce.responses.PaginatedResponse;
import com.aadil.ecommerce.services.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Validated
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/invitation-link")
    public ResponseEntity<APIResponse<String>> adminInvitationLink(@Valid @RequestBody InvitationDTO dto) {

        String inviteLink = adminService.invitationCreation(dto);

        APIResponse<String> apiResponse = APIResponse.<String>builder()
                .success(true)
                .code(201)
                .message("Invitation link is created successfully")
                .data(inviteLink)
                .build();

        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @GetMapping("/all")
    public ResponseEntity<APIResponse<PaginatedResponse<List<UserResponseDTO>>>> getAllUsers(
            @RequestParam(required = false, name = "Search") String search,
            @RequestParam(required = false, name = "PageNumber", defaultValue = "0") int pageNumber,
            @RequestParam(required = false, name = "Size", defaultValue = "10") int size,
            @RequestParam(required = false, name = "SortField", defaultValue = "ID") SortFields userSortField,
            @RequestParam(required = false, name = "SortDirection", defaultValue = "ASC") SortDirections sortDirection) {

        PaginatedResponse<List<UserResponseDTO>> contents =
                adminService.getAllUsers(search, pageNumber, size, userSortField, sortDirection);

        APIResponse<PaginatedResponse<List<UserResponseDTO>>> apiResponse =
                APIResponse.<PaginatedResponse<List<UserResponseDTO>>>builder()
                        .success(true)
                        .code(200)
                        .message("All Users retrieved successfully")
                        .data(contents)
                        .build();

        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @DeleteMapping("/all/{ids}")
    public ResponseEntity<Void> deleteAllById(@PathVariable List<Long> ids, @RequestParam boolean confirm) {
        adminService.deleteUsersById(ids, confirm);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, @RequestParam boolean confirm) {
        adminService.deleteUser(id, confirm);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/make-admin")
    public ResponseEntity<APIResponse<UserResponseDTO>> promoteToAdmin(@PathVariable Long id) {
        UserResponseDTO userToAdmin = adminService.promoteToAdmin(id);

        APIResponse<UserResponseDTO> apiResponse = APIResponse.<UserResponseDTO>builder()
                .success(true)
                .code(200)
                .message("Conversion of USER to ADMIN is successfully completed")
                .data(userToAdmin)
                .build();

        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/admin-demotion/{id}")
    public ResponseEntity<APIResponse<UserResponseDTO>> adminDemotion(@PathVariable Long id) {
        UserResponseDTO demotedAdmin = adminService.adminDemotion(id);

        APIResponse<UserResponseDTO> apiResponse = APIResponse.<UserResponseDTO>builder()
                .success(true)
                .code(200)
                .message("Conversion of ADMIN to USER is successfully completed")
                .data(demotedAdmin)
                .build();

        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}
