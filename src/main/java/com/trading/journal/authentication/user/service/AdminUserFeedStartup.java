package com.trading.journal.authentication.user.service;

import com.trading.journal.authentication.registration.UserRegistration;
import com.trading.journal.authentication.user.properties.AdminUserCondition;
import com.trading.journal.authentication.user.properties.AdminUserProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
@Conditional(AdminUserCondition.class)
public class AdminUserFeedStartup implements ApplicationListener<ApplicationReadyEvent> {

    private final ApplicationAdminUserService applicationAdminUserService;

    private final AdminUserProperties adminUserProperties;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (applicationAdminUserService.thereIsAdmin()) {
            log.info("Admin has been already create previously");
        } else {
            UserRegistration userRegistration = new UserRegistration("Admin", "Administrator", "admin", adminUserProperties.email(), null, null);
            applicationAdminUserService.createAdmin(userRegistration);
            log.info("Admin user created during startup");
        }
    }
}
