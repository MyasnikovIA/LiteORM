package persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)  // Specifies that the annotation should be retained at runtime
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})  // Specifies that the annotation can only be applied to classes
public @interface Id {
    // SMALLSERIAL, SERIAL, и BIGSERIAL
    // SMALLSERIAL	2 байта	от 1 до 32 767
    // SERIAL   	4 байта	от 1 до 2 147 483 647
    // BIGSERIAL	8 байт	от 1 до 9 223 372 036 854 775 807
    String type() default "SERIAL";
}

