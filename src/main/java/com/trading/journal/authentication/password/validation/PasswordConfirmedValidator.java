package com.trading.journal.authentication.password.validation;

import lombok.NoArgsConstructor;

import java.util.Optional;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@NoArgsConstructor
public class PasswordConfirmedValidator implements ConstraintValidator<PasswordConfirmed, PasswordAndConfirmation> {

    @Override
    public boolean isValid(PasswordAndConfirmation userRegistration, ConstraintValidatorContext context) {
        String password = Optional.ofNullable(userRegistration).map(PasswordAndConfirmation::getPassword).orElse("");
        String confirmedPassword = Optional.ofNullable(userRegistration).map(PasswordAndConfirmation::getConfirmPassword)
                .orElse("");
        return password.equals(confirmedPassword);
    }
}
