package com.example.ronproject.auth;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.ronproject.security.JwtService;
import com.example.ronproject.user.CurrentUser;
import com.example.ronproject.user.UserAccount;
import com.example.ronproject.user.UserAccountRepository;
import com.example.ronproject.user.UserRole;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        String username = request.username().trim();

        if (userAccountRepository.existsByEmailAndDeletedFalse(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already registered");
        }
        if (userAccountRepository.existsByUsernameAndDeletedFalse(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is already taken");
        }

        UserAccount userAccount = new UserAccount();
        userAccount.setEmail(email);
        userAccount.setUsername(username);
        userAccount.setPasswordHash(passwordEncoder.encode(request.password()));
        userAccount.setRole(UserRole.USER);
        UserAccount savedUser;
        try {
            savedUser = userAccountRepository.saveAndFlush(userAccount);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email or username is already taken");
        }
        log.info("New user registered: {}", email);

        CurrentUser currentUser = new CurrentUser(savedUser);
        return new AuthResponse(
                jwtService.generateToken(currentUser),
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail()
        );
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        log.info("Login attempt for: {}", email);
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));
        CurrentUser currentUser = (CurrentUser) auth.getPrincipal();
        return new AuthResponse(
                jwtService.generateToken(currentUser),
                currentUser.getId(),
                currentUser.getDisplayName(),
                currentUser.getUsername()
        );
    }

    public void logout(String token) {
        jwtService.revokeToken(token);
        log.info("User logged out");
    }

    @Transactional
    public void deleteAccount(CurrentUser currentUser, String token) {
        UserAccount userAccount = userAccountRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String suffix = "_deleted_" + UUID.randomUUID();
        userAccount.setEmail(userAccount.getEmail() + suffix);
        userAccount.setUsername(userAccount.getUsername() + suffix);
        userAccount.setDeleted(true);
        jwtService.revokeToken(token);
        log.info("User account soft-deleted: {}", currentUser.getUsername());
    }
}
