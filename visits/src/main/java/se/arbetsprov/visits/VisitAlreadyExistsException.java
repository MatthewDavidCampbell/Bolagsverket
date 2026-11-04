package se.arbetsprov.visits;

/** Thrown when a user marks a country they have already marked. */
public class VisitAlreadyExistsException extends RuntimeException {

    private final String countryCode;

    public VisitAlreadyExistsException(String countryCode) {
        super("Country " + countryCode + " is already marked as visited");
        this.countryCode = countryCode;
    }

    public String countryCode() {
        return countryCode;
    }
}
