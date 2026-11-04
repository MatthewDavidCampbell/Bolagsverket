package se.arbetsprov.service.visits;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Documents the {@code X-User-Id} header the same way on every visits operation. */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Parameter(in = ParameterIn.HEADER, name = VisitsController.USER_HEADER, required = true, example = "alice",
        description = "The calling user, 1 to 64 characters. Stands in for an identity taken from an "
                + "authentication token; no real authentication is done.")
@interface UserId {
}
