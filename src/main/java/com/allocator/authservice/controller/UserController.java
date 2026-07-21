package com.allocator.authservice.controller;

import com.allocator.authservice.dto.ProfileUpdateRequest;
import com.allocator.authservice.dto.UserDto;
import com.allocator.authservice.repository.UserRepository;
import com.allocator.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<UserDto>> getUsers() {
        log.info("Received request to fetch all users");
        return ResponseEntity.ok(authService.getUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID id) {
        log.info("Received request to fetch user: {}", id);
        return ResponseEntity.ok(authService.getUserById(id));
    }

    /** @mention autocomplete — any authenticated user, minimal fields only. */
    @GetMapping("/search")
    public ResponseEntity<List<com.allocator.authservice.dto.MentionUserDto>> searchUsers(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(authService.searchUsersForMention(q, limit));
    }

    /** Returns the authenticated user's own profile. */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        log.info("Fetching profile for: {}", email);
        return ResponseEntity.ok(authService.getUserByEmail(email));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateProfile(@RequestBody ProfileUpdateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = authentication.getName();
        log.info("Received profile update request for user: {}", currentEmail);
        return ResponseEntity.ok(authService.updateProfileByEmail(currentEmail, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID id) {
        log.info("Received request to delete user: {}", id);
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id);
        }
        userRepository.deleteById(id);
    }

    @PostMapping("/{id}/force-password-reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forcePasswordReset(@PathVariable UUID id) {
        log.info("Forcing password reset for user: {}", id);
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
        user.setMustChangePassword(true);
        userRepository.save(user);
    }
}
