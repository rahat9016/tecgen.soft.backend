package com.tecgensoft.service;

import com.tecgensoft.common.utils.Util;
import com.tecgensoft.http.dtos.requests.ChangePasswordRequest;
import com.tecgensoft.http.validations.ValidationUtil;
import com.tecgensoft.jpa.daos.UserDao;
import com.tecgensoft.jpa.repositories.UserRepository;
import com.tecgensoft.mapper.UserMapper;
import com.tecgensoft.service.entities.User;
import com.tecgensoft.service.exceptions.InvalidCredentialsException;
import com.tecgensoft.service.exceptions.UserEmailAlreadyExistsException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final UserService userService;

    // SIGNUP
    public User signup(User user) {

        String normalizedEmail = normalizeEmail(user.email());

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new UserEmailAlreadyExistsException(normalizedEmail);
        }

        // Hash the password with a salt
        String salt = Util.generateSalt();
        String hashedPassword = Util.hashPassword(user.password(), salt);
        // Create a new User object with the hashed password
        User hashedUser = user
                .withEmail(normalizedEmail)
                .withPassword(hashedPassword);

        UserDao userDao = UserMapper.toDao(hashedUser, salt);

        // Save the user to the database
        UserDao savedUser = userRepository.save(userDao);

        // Return the saved user as a User entity
        return UserMapper.toEntity(savedUser);
    }

    // LOGIN
    public User login(User user) {
        String normalizedEmail = normalizeEmail(user.email());

        UserDao userDao = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        String hashedPassword = Util.hashPassword(user.password(), userDao.getSalt());

        UserDao verifiedUser = userRepository
                .findByEmailAndPassword(userDao.getEmail(), hashedPassword)
                .orElseThrow(InvalidCredentialsException::new);

        return userService.getUser(verifiedUser.getId());
    }
    // LOGOUT
    public void logout() {
        // Implementation for logout
    }
    @Transactional
    public User changePassword(
            UUID userId,
            ChangePasswordRequest request
    ) {

        // 1. FIND AUTHENTICATED USER

        UserDao user = userRepository
                .findById(userId)
                .orElseThrow(
                        InvalidCredentialsException::new
                );

        // 2. VERIFY OLD PASSWORD

        boolean validOldPassword =
                Util.verifyPassword(
                        request.oldPassword(),
                        user.getPassword()
                );

        if (!validOldPassword) {
            throw new InvalidCredentialsException();
        }

        // 3. VALIDATE NEW PASSWORD (length + must differ)

        ValidationUtil.validateNewOrganizationAdminPassword(
                request.newPassword(),
                user.getPassword()
        );

        // 4. GENERATE NEW SALT AND HASH

        String newSalt = Util.generateSalt();

        String newHashedPassword = Util.hashPassword(
                request.newPassword(),
                newSalt
        );

        // 5. UPDATE CREDENTIALS

        user.setSalt(newSalt);
        user.setPassword(newHashedPassword);

        // 6. SAVE CHANGES

        UserDao savedUser =
                userRepository.saveAndFlush(user);

        log.info(
                "Password changed: userId={}",
                savedUser.getId()
        );

        return UserMapper.toEntity(savedUser);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
