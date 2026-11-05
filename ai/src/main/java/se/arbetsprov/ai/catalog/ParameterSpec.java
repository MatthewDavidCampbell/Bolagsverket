package se.arbetsprov.ai.catalog;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * One parameter of an operation: where it goes in the request and which values are allowed.
 * Every value the LLM suggests passes through {@link #validate(Object)} before a request is built.
 *
 * @param name        parameter name, as the service expects it
 * @param location    query string, path or JSON body
 * @param type        text or whole number
 * @param required    whether the request cannot be built without it
 * @param allowed     allowed text values (empty for any); matched ignoring letter case
 * @param pattern     regular expression text values must match, or null
 * @param min         smallest whole number, or null
 * @param max         largest whole number (or longest text), or null
 * @param description what the value means, shown to the LLM
 */
public record ParameterSpec(
        String name,
        Location location,
        Type type,
        boolean required,
        List<String> allowed,
        String pattern,
        Integer min,
        Integer max,
        String description) {

    public enum Location { QUERY, PATH, BODY }

    public enum Type { TEXT, NUMBER }

    public ParameterSpec {
        Objects.requireNonNull(name, "name");
        allowed = List.copyOf(allowed);
    }

    /** An optional text value limited to {@code allowed}. */
    static ParameterSpec choice(String name, Location location, String description, String... allowed) {
        return new ParameterSpec(name, location, Type.TEXT, false, List.of(allowed), null, null, null, description);
    }

    /** An optional whole number between {@code min} and {@code max}. */
    static ParameterSpec number(String name, Location location, int min, int max, String description) {
        return new ParameterSpec(name, location, Type.NUMBER, false, List.of(), null, min, max, description);
    }

    /** A required ISO 3166-1 alpha-3 country code. */
    static ParameterSpec countryCode(Location location) {
        return new ParameterSpec("countryCode", location, Type.TEXT, true, List.of(), "[A-Za-z]{3}", null, null,
                "ISO 3166-1 alpha-3 country code in English, for example SWE for Sweden, NOR for Norway");
    }

    /** An optional free text of at most {@code maxLength} characters. */
    static ParameterSpec text(String name, Location location, int maxLength, String description) {
        return new ParameterSpec(name, location, Type.TEXT, false, List.of(), null, null, maxLength, description);
    }

    /**
     * Checks a suggested value and returns it in canonical form (for example "europe" becomes
     * "Europe" and "swe" becomes "SWE").
     *
     * @throws InvalidParametersException when the value is not allowed
     */
    public Object validate(Object value) {
        return switch (type) {
            case NUMBER -> validateNumber(value);
            case TEXT -> validateText(value);
        };
    }

    private Object validateNumber(Object value) {
        long number;
        if (value instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) {
            number = n.longValue();
        } else if (value instanceof String s && s.strip().matches("-?\\d+")) {
            number = Long.parseLong(s.strip());
        } else {
            throw new InvalidParametersException(name + " must be a whole number, was " + value);
        }
        if ((min != null && number < min) || (max != null && number > max)) {
            throw new InvalidParametersException(name + " must be between " + min + " and " + max + ", was " + number);
        }
        return (int) number;
    }

    private Object validateText(Object value) {
        String text = String.valueOf(value).strip();
        if (!allowed.isEmpty()) {
            return allowed.stream()
                    .filter(a -> a.equalsIgnoreCase(text))
                    .findFirst()
                    .orElseThrow(() -> new InvalidParametersException(
                            name + " must be one of " + String.join(", ", allowed) + ", was " + text));
        }
        if (pattern != null) {
            if (!Pattern.matches(pattern, text)) {
                throw new InvalidParametersException(name + " is not valid: " + text);
            }
            return text.toUpperCase(Locale.ROOT);
        }
        if (max != null && text.length() > max) {
            throw new InvalidParametersException(name + " must be at most " + max + " characters");
        }
        return text;
    }
}
