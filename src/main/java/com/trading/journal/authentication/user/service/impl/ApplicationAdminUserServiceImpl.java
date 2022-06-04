package com.trading.journal.authentication.user.service.impl;

import com.trading.journal.authentication.authority.AuthoritiesHelper;
import com.trading.journal.authentication.authority.AuthorityCategory;
import com.trading.journal.authentication.authority.UserAuthority;
import com.trading.journal.authentication.authority.service.UserAuthorityService;
import com.trading.journal.authentication.registration.AdminRegistration;
import com.trading.journal.authentication.user.ApplicationUser;
import com.trading.journal.authentication.user.service.ApplicationAdminUserService;
import com.trading.journal.authentication.user.service.ApplicationUserRepository;
import com.trading.journal.authentication.verification.VerificationType;
import com.trading.journal.authentication.verification.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationAdminUserServiceImpl implements ApplicationAdminUserService {

    private final ApplicationUserRepository applicationUserRepository;

    private final UserAuthorityService userAuthorityService;

    private final VerificationService verificationService;

    private final PasswordEncoder encoder;

    @Override
    public Mono<Boolean> thereIsAdmin() {
        List<String> roles = AuthoritiesHelper.getByCategory(AuthorityCategory.ADMINISTRATOR).stream().map(AuthoritiesHelper::getLabel).toList();
        return applicationUserRepository.countAdmins(roles)
                .map(num -> num > 0);
    }

    @Override
    public Mono<Void> createAdmin(@Valid AdminRegistration adminRegistration) {
        return adminUser(adminRegistration)
                .flatMap(applicationUserRepository::save)
                .flatMap(userAuthorityService::saveAdminUserAuthorities)
                .map(UserAuthority::getUserId)
                .flatMap(applicationUserRepository::findById)
                .flatMap(applicationUser -> verificationService.send(VerificationType.ADMIN_REGISTRATION, applicationUser));
    }

    private Mono<ApplicationUser> adminUser(AdminRegistration adminRegistration) {
        return Mono.just(ApplicationUser.builder()
                .userName(adminRegistration.userName())
                .password(encoder.encode(adminRegistration.email()))
                .firstName(adminRegistration.firstName())
                .lastName(adminRegistration.lastName())
                .email(adminRegistration.email())
                .enabled(false)
                .verified(false)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
