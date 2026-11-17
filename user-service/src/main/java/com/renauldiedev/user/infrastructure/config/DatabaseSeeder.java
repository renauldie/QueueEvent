package com.renauldiedev.user.infrastructure.config;

import com.renauldiedev.user.domain.user.Role;
import com.renauldiedev.user.domain.user.RoleRepository;
import com.renauldiedev.user.domain.user.User;
import com.renauldiedev.user.domain.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing User Service database seeds...");

        if (roleRepository.count() == 0) {
            roleRepository.saveAll(List.of(
                    Role.builder().name("ROLE_USER").description("Standard customer user").build(),
                    Role.builder().name("ROLE_ADMIN").description("System administrator").build()
            ));
            log.info("Seeded default roles into user_db.");
        }

        if (userRepository.count() == 0) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
            Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow();

            userRepository.saveAll(List.of(
                    User.builder()
                            .email("admin@eventflow.com")
                            .password(passwordEncoder.encode("admin123"))
                            .fullName("System Administrator")
                            .role(adminRole)
                            .build(),
                    User.builder()
                            .email("user@eventflow.com")
                            .password(passwordEncoder.encode("user123"))
                            .fullName("Demo Customer")
                            .role(userRole)
                            .build()
            ));
            log.info("Seeded default users (admin@eventflow.com, user@eventflow.com) into user_db.");
        }
    }
}
