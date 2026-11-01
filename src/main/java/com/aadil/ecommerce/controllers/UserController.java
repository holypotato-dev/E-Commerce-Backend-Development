package com.aadil.ecommerce.controllers;

import com.aadil.ecommerce.responses.APIResponse;
import com.aadil.ecommerce.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Validated
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/invitation-accept/{rawToken}")
    public ResponseEntity<APIResponse<String>> invitationAccept(@PathVariable String rawToken) {

        String invitationAccept = userService.invitationAcceptance(rawToken);

        APIResponse<String> apiResponse = APIResponse.<String>builder()
                .success(true)
                .code(200)
                .message(invitationAccept)
                .build();

        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}
