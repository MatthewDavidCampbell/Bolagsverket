package se.arbetsprov.visits;

/** Thrown when a visit names a country code the country directory does not know. */
public class UnknownCountryException extends RuntimeException {

    private final String countryCode;

    public UnknownCountryException(String countryCode) {
        super("Unknown country code: " + countryCode);
        this.countryCode = countryCode;
    }

    public String countryCode() {
        return countryCode;
    }
}
