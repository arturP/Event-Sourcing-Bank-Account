package io.artur.eventsourcing.api.controller;

import io.artur.eventsourcing.api.dto.AuthResponse;
import io.artur.eventsourcing.api.dto.LoginRequest;
import io.artur.eventsourcing.security.JwtService;
import io.artur.eventsourcing.security.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication management endpoints")
public class AuthController {
    
    private final UserService userService;
    private final JwtService jwtService;
    
    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }
    
    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Authenticate user and return JWT token")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        boolean isValid = userService.validateUser(loginRequest.getUsername(), loginRequest.getPassword());
        
        if (!isValid) {
            return ResponseEntity.badRequest().body("Invalid username or password");
        }
        
        UserDetails userDetails = userService.loadUserByUsername(loginRequest.getUsername());
        String token = jwtService.generateToken(userDetails);
        
        AuthResponse response = new AuthResponse(
            token,
            userDetails.getUsername(),
            jwtService.getExpirationDateTime(token)
        );
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/demo-users")
    @Operation(summary = "Get demo users", description = "Get list of available demo users for testing")
    public ResponseEntity<?> getDemoUsers() {
        return ResponseEntity.ok(new String[][]{
            {"admin", "admin123", "ADMIN, USER"},
            {"user", "user123", "USER"},
            {"demo", "demo123", "USER"}
        });
    }
}