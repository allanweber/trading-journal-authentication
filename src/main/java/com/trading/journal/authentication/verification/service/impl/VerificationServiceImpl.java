package com.trading.journal.authentication.verification.service.impl;

import com.trading.journal.authentication.ApplicationException;
import com.trading.journal.authentication.user.User;
import com.trading.journal.authentication.user.UserRepository;
import com.trading.journal.authentication.verification.Verification;
import com.trading.journal.authentication.verification.VerificationRepository;
import com.trading.journal.authentication.verification.VerificationRequest;
import com.trading.journal.authentication.verification.VerificationType;
import com.trading.journal.authentication.verification.properties.VerificationProperties;
import com.trading.journal.authentication.verification.service.HashProvider;
import com.trading.journal.authentication.verification.service.VerificationEmailService;
import com.trading.journal.authentication.verification.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService {

    private final VerificationRepository verificationRepository;
    private final VerificationEmailService verificationEmailService;

    private final UserRepository userRepository;

    private final HashProvider hashProvider;

    private final VerificationProperties verificationProperties;

    @Override
    public void send(VerificationType verificationType, User applicationUser) {
        Verification verification = verificationRepository.getByTypeAndEmail(verificationType, applicationUser.getEmail())
                .orElseGet(() -> Verification.builder().email(applicationUser.getEmail()).type(verificationType).build());
        if (doNotSendVerification(verification)) {
            return;
        }
        verification = verification.renew(hashProvider.generateHash(verification.getEmail()));
        verification = verificationRepository.save(verification);
        verificationEmailService.sendEmail(verification, applicationUser);
    }

    @Override
    public Verification retrieve(String hash) {
        String email = hashProvider.readHashValue(hash);
        return verificationRepository.getByHashAndEmail(hash, email)
                .orElseThrow(() -> new ApplicationException(HttpStatus.BAD_REQUEST, "Request is invalid"));
    }

    @Override
    public void verify(Verification verification) {
        if (shouldSendChangePassword(verification)) {
            sendChangePassword(verification);
        }
        verificationRepository.delete(verification);
    }

    @Override
    public List<Verification> getByEmail(String email) {
        return verificationRepository.getByEmail(email);
    }

    @Override
    public Verification create(VerificationRequest verificationRequest) {
        User user = userRepository.findByEmail(verificationRequest.email())
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, String.format("User %s does not exist", verificationRequest.email())));

        this.send(verificationRequest.verificationType(), user);
        return verificationRepository.getByTypeAndEmail(verificationRequest.verificationType(), verificationRequest.email())
                .orElseThrow(() -> new ApplicationException("Verification not created"));
    }

    private void sendChangePassword(Verification verification) {
        User user = userRepository.findByEmail(verification.getEmail())
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, String.format("User %s does not exist", verification.getEmail())));
        this.send(VerificationType.CHANGE_PASSWORD, user);
    }

    private static boolean shouldSendChangePassword(Verification verification) {
        return VerificationType.ADMIN_REGISTRATION.equals(verification.getType())
                || VerificationType.NEW_ORGANISATION_USER.equals(verification.getType());
    }

    private boolean doNotSendVerification(Verification verification) {
        return isUserRegistration(verification) && !verificationProperties.isEnabled();
    }

    private static boolean isUserRegistration(Verification verification) {
        return VerificationType.ADMIN_REGISTRATION.equals(verification.getType())
                || VerificationType.REGISTRATION.equals(verification.getType())
                || VerificationType.NEW_ORGANISATION_USER.equals(verification.getType());
    }
}
