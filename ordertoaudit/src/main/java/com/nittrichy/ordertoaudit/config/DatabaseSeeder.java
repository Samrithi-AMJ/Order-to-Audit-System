package com.nittrichy.ordertoaudit.config;

import com.nittrichy.ordertoaudit.entity.Role;
import com.nittrichy.ordertoaudit.entity.User;
import com.nittrichy.ordertoaudit.enums.RoleName;
import com.nittrichy.ordertoaudit.repository.RoleRepository;
import com.nittrichy.ordertoaudit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Seeds role metadata only in the explicitly selected demo profile. */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.username:}")
    private String bootstrapAdminUsername;

    @Value("${app.bootstrap-admin.password:}")
    private String bootstrapAdminPassword;

    @Override
    public void run(String... args) {
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(Role.builder()
                        .name(roleName)
                        .description(roleName.name() + " role for Order-to-Audit system")
                        .build());
            }
        }
        if (bootstrapAdminUsername != null && !bootstrapAdminUsername.isBlank()
                && bootstrapAdminPassword != null && bootstrapAdminPassword.length() >= 12
                && !userRepository.existsByUsername(bootstrapAdminUsername)) {
            Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
            userRepository.save(User.builder()
                    .username(bootstrapAdminUsername)
                    .password(passwordEncoder.encode(bootstrapAdminPassword))
                    .fullName("System Administrator")
                    .roles(java.util.Set.of(adminRole))
                    .active(true)
                    .build());
            log.info("Created the explicitly configured bootstrap administrator account.");
        } else if (bootstrapAdminUsername != null && !bootstrapAdminUsername.isBlank()) {
            log.warn("Bootstrap administrator was not created; provide a new account name and a password of at least 12 characters.");
        }
    }
}
