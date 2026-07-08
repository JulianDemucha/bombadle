package com.bombadle.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Requires at least 1 uppercase letter, 1 digit and 1 special character. Null passes (see @NotBlank). */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {
    String message() default "Hasło musi zawierać co najmniej jedną wielką literę, jedną cyfrę i jeden znak specjalny";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
