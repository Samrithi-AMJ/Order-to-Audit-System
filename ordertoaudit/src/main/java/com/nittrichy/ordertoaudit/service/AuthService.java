package com.nittrichy.ordertoaudit.service;

import com.nittrichy.ordertoaudit.dto.AuthResponse;
import com.nittrichy.ordertoaudit.dto.LoginRequest;
import com.nittrichy.ordertoaudit.dto.RegisterRequest;
import com.nittrichy.ordertoaudit.dto.UserDto;
import com.nittrichy.ordertoaudit.entity.Role;
import com.nittrichy.ordertoaudit.entity.User;
import com.nittrichy.ordertoaudit.enums.RoleName;
import com.nittrichy.ordertoaudit.exception.BusinessRuleException;
import com.nittrichy.ordertoaudit.repository.RoleRepository;
import com.nittrichy.ordertoaudit.repository.UserRepository;
import com.nittrichy.ordertoaudit.security.JwtTokenProvider;
import com.nittrichy.ordertoaudit.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return AuthResponse.builder()
                .token(jwt)
                .id(userPrincipal.getId())
                .username(userPrincipal.getUsername())
                .fullName(userPrincipal.getFullName())
                .email(userPrincipal.getEmail())
                .roles(roles)
                .build();
    }

    @Transactional
    public UserDto register(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new BusinessRuleException("Username '" + registerRequest.getUsername() + "' is already taken");
        }

        if (registerRequest.getEmail() != null && userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BusinessRuleException("Email '" + registerRequest.getEmail() + "' is already registered");
        }

        // Public self-registration must never grant a caller-selected privileged role.
        RoleName registrationRole = RoleName.SALESMAN;
        Role role = roleRepository.findByName(registrationRole)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(registrationRole)
                        .description(registrationRole.name() + " role")
                        .build()));
        Set<Role> roles = Set.of(role);

        User user = User.builder()
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .fullName(registerRequest.getFullName())
                .email(registerRequest.getEmail())
                .phone(registerRequest.getPhone())
                .roles(roles)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        return mapToDto(savedUser);
    }

    public User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return userRepository.findById(principal.getId()).orElse(null);
        }
        return null;
    }

    public UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .active(user.isActive())
                .roles(user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()))
                .build();
    }
}

