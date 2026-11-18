package cat.copernic.nothinganpri.validacio;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = ProvinciaValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProvinciaValida {

    String message() default "La província no és vàlida";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    
}