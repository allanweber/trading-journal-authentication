package com.trading.journal.authentication.user.service.impl;

import com.trading.journal.authentication.ApplicationException;
import com.trading.journal.authentication.password.service.PasswordService;
import com.trading.journal.authentication.registration.UserRegistration;
import com.trading.journal.authentication.tenancy.Tenancy;
import com.trading.journal.authentication.user.User;
import com.trading.journal.authentication.user.UserRepository;
import com.trading.journal.authentication.user.UserInfo;
import com.trading.journal.authentication.user.service.UserService;
import com.trading.journal.authentication.userauthority.UserAuthority;
import com.trading.journal.authentication.userauthority.service.UserAuthorityService;
import com.trading.journal.authentication.verification.properties.VerificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final UserAuthorityService userAuthorityService;

    private final VerificationProperties verificationProperties;

    private final PasswordService passwordService;

    @Override
    public User getUserByEmail(@NotBlank String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(String.format("User %s does not exist", email)));
    }

    @Override
    public User createNewUser(@NotNull UserRegistration userRegistration, Tenancy tenancy) {
        Boolean validUser = validateNewUser(userRegistration.getUserName(), userRegistration.getEmail());
        if (validUser) {
            User user = userRepository.save(buildUser(userRegistration, tenancy));
            List<UserAuthority> userAuthorities = userAuthorityService.saveCommonUserAuthorities(user);
            user.setUserAuthorities(userAuthorities);
            return user;
        } else {
            throw new ApplicationException("User name or email already exist");
        }
    }

    @Override
    public Boolean validateNewUser(@NotNull String userName, @NotBlank String email) {
        Boolean userNameExists = userNameExists(userName);
        Boolean emailExists = emailExists(email);
        return !userNameExists && !emailExists;
    }

    @Override
    public Boolean userNameExists(@NotBlank String userName) {
        return userRepository.existsByUserName(userName);
    }

    @Override
    public Boolean emailExists(@NotBlank String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public UserInfo getUserInfo(@NotBlank String email) {
        User applicationUser = this.getUserByEmail(email);
        return new UserInfo(applicationUser);
    }

    @Override
    public void verifyUser(@NotBlank String email) {
        User applicationUser = this.getUserByEmail(email);
        applicationUser.enable();
        applicationUser.verify();
        userRepository.save(applicationUser);
    }

    @Override
    public void unprovenUser(String email) {
        User applicationUser = this.getUserByEmail(email);
        applicationUser.unproven();
        userRepository.save(applicationUser);
    }


    @Override
    public User changePassword(@NotBlank String email, @NotBlank String password) {
        User applicationUser = this.getUserByEmail(email);
        applicationUser.changePassword(passwordService.encodePassword(password));
        return userRepository.save(applicationUser);
    }

    private User buildUser(UserRegistration userRegistration, Tenancy tenancy) {
        boolean enabledAndVerified = !verificationProperties.isEnabled();
        return User.builder()
                .tenancy(tenancy)
                .userName(userRegistration.getUserName())
                .password(passwordService.encodePassword(userRegistration.getPassword()))
                .firstName(userRegistration.getFirstName())
                .lastName(userRegistration.getLastName())
                .email(userRegistration.getEmail())
                .enabled(enabledAndVerified)
                .verified(enabledAndVerified)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
