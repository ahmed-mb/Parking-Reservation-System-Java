package com.ahmedbahaj.parking.exception;

import com.ahmedbahaj.parking.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
    }

    @Test
    @DisplayName("handleValidationErrors - should return 400 with field errors")
    void handleValidationErrors_shouldReturn400() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "must not be null");
        
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex, webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Validation Error", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("must not be null"));
    }

    @Test
    @DisplayName("handleResourceNotFound - should return 404")
    void handleResourceNotFound_shouldReturn404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex, webRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Not Found", response.getBody().getError());
        assertEquals("User not found", response.getBody().getMessage());
    }

    @Test
    @DisplayName("handleInsufficientCredit - should return 400")
    void handleInsufficientCredit_shouldReturn400() {
        InsufficientCreditException ex = new InsufficientCreditException("Not enough credit");

        ResponseEntity<ErrorResponse> response = handler.handleInsufficientCredit(ex, webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Insufficient Credit", response.getBody().getError());
    }

    @Test
    @DisplayName("handleParkingNotAvailable - should return 409")
    void handleParkingNotAvailable_shouldReturn409() {
        ParkingNotAvailableException ex = new ParkingNotAvailableException("Spot taken");

        ResponseEntity<ErrorResponse> response = handler.handleParkingNotAvailable(ex, webRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Parking Not Available", response.getBody().getError());
    }

    @Test
    @DisplayName("handleInvalidCredentials - should return 401")
    void handleInvalidCredentials_shouldReturn401() {
        InvalidCredentialsException ex = new InvalidCredentialsException("Bad password");

        ResponseEntity<ErrorResponse> response = handler.handleInvalidCredentials(ex, webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Unauthorized", response.getBody().getError());
    }

    @Test
    @DisplayName("handleDuplicateEmail - should return 409")
    void handleDuplicateEmail_shouldReturn409() {
        DuplicateEmailException ex = new DuplicateEmailException("Email exists");

        ResponseEntity<ErrorResponse> response = handler.handleDuplicateEmail(ex, webRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Duplicate Email", response.getBody().getError());
    }

    @Test
    @DisplayName("handleAccessDenied - should return 403")
    void handleAccessDenied_shouldReturn403() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex, webRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Forbidden", response.getBody().getError());
    }

    @Test
    @DisplayName("handleBadCredentials - should return 401")
    void handleBadCredentials_shouldReturn401() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<ErrorResponse> response = handler.handleBadCredentials(ex, webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Unauthorized", response.getBody().getError());
        assertEquals("Invalid email or password", response.getBody().getMessage());
    }

    @Test
    @DisplayName("handleGlobalException - should return 500 without exposing details")
    void handleGlobalException_shouldReturn500() {
        Exception ex = new RuntimeException("Internal database error - sensitive info");

        ResponseEntity<ErrorResponse> response = handler.handleGlobalException(ex, webRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Internal Server Error", response.getBody().getError());
        // Should NOT expose the actual error message
        assertFalse(response.getBody().getMessage().contains("database"));
        assertEquals("An unexpected error occurred. Please try again later.", response.getBody().getMessage());
    }
}
