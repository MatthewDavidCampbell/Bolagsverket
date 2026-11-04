package se.arbetsprov.visits;

/** Thrown when a user changes a visit they have not made. */
public class VisitNotFoundException extends RuntimeException {

    private final String countryCode;

    public VisitNotFoundException(String countryCode) {
        super("Country " + countryCode + " is not marked as visited");
        this.countryCode = countryCode;
    }

    public String countryCode() {
        return countryCode;
    }
}
