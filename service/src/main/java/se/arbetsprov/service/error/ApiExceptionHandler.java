package se.arbetsprov.service.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import se.arbetsprov.visits.UnknownCountryException;
import se.arbetsprov.visits.VisitAlreadyExistsException;
import se.arbetsprov.visits.VisitNotFoundException;

/**
 * Turns failures into RFC 9457 problem details. The base class already covers Spring MVC's own
 * errors (missing header, wrong parameter type, unknown route, {@code ResponseStatusException});
 * the methods below map the libraries' exceptions.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidInput(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(UnknownCountryException.class)
    ProblemDetail unknownCountry(UnknownCountryException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(VisitNotFoundException.class)
    ProblemDetail visitNotFound(VisitNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(VisitAlreadyExistsException.class)
    ProblemDetail visitAlreadyExists(VisitAlreadyExistsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
