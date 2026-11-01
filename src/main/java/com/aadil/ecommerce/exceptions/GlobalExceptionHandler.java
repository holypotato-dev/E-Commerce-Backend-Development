package com.aadil.ecommerce.exceptions;

import com.aadil.ecommerce.responses.APIResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private <T> ResponseEntity<APIResponse<T>> buildError(HttpStatus status, String message, T data) {
        APIResponse<T> apiResponse = APIResponse.<T>builder()
                .success(false)
                .message(message)
                .code(status.value())
                .data(data)
                .createdAt(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(apiResponse, status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse<Map<String, String>>> handlesMethodArgumentNotValid(
            MethodArgumentNotValidException methodArgumentNotValidException) {
        Map<String, String> errors = new HashMap<>();

        methodArgumentNotValidException.getBindingResult().getFieldErrors()
                .forEach(fieldError -> errors.put(fieldError.getField(), fieldError.getDefaultMessage()));

        return buildError(HttpStatus.BAD_REQUEST, "Validation Failed", errors);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<APIResponse<Void>> handlesAccessDenied(AccessDeniedException ex) {
        return buildError(HttpStatus.FORBIDDEN, "Access Denied: You do not have permission to access this resource", null);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<APIResponse<Void>> handlesAlreadyExists(UserAlreadyExistsException e) {
        return buildError(HttpStatus.CONFLICT, e.getMessage(), null);
    }

    @ExceptionHandler({AuthenticationException.class, InvalidCredentialException.class, JwtException.class})
    public ResponseEntity<APIResponse<Void>> handlesAuthenticationFailures(Exception ex) {
        return buildError(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
    }

    @ExceptionHandler({ResourceNotFoundException.class, UserNotFoundException.class,
            ProductNotFoundException.class, InvitationNotFoundException.class, UsernameNotFoundException.class})
    public ResponseEntity<APIResponse<Void>> handlesNotFound(RuntimeException exception) {
        return buildError(HttpStatus.NOT_FOUND, exception.getMessage(), null);
    }

    @ExceptionHandler({UserAlreadyAnAdminException.class, RequestPendingException.class,
            AlreadyAUserException.class, InsufficientStockException.class,
            InvitationAlreadyExistsException.class})
    public ResponseEntity<APIResponse<Void>> handlesConflicts(RuntimeException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<APIResponse<Void>> handlesExpiredJwt(ExpiredJwtException expiredJwtException) {
        return buildError(HttpStatus.UNAUTHORIZED, expiredJwtException.getMessage(), null);
    }

    @ExceptionHandler(InvitationExpiredException.class)
    public ResponseEntity<APIResponse<Void>> handlesInvitationExpired(InvitationExpiredException e) {
        return buildError(HttpStatus.GONE, e.getMessage(), null);
    }

    @ExceptionHandler({InvalidRequestException.class, InvalidInvitationException.class,
            IllegalArgumentException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<APIResponse<Void>> handlesBadRequest(Exception e) {
        return buildError(HttpStatus.BAD_REQUEST, e.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse<Void>> handleUnhandledExceptions(Exception ex) {
        log.error("Unhandled exception: ", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred", null);
    }
}
