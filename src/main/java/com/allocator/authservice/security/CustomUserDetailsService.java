package com.allocator.authservice.security;

import com.allocator.authservice.model.User;
import com.allocator.authservice.model.UserRole;
import com.allocator.authservice.repository.UserRepository;
import com.allocator.authservice.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        List<UserRole> userRoles = userRoleRepository.findByUser(user);

        Set<String> roles = userRoles.stream()
                .map(ur -> "ROLE_" + ur.getRole().getName().name())
                .collect(Collectors.toSet());

        Set<UUID> brandIds = userRoles.stream()
                .map(ur -> ur.getBrand().getId())
                .collect(Collectors.toSet());

        return new CustomUserDetails(user, roles, brandIds);
    }
}
