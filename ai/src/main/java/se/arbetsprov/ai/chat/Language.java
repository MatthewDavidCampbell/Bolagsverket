package se.arbetsprov.ai.chat;

import java.util.Locale;
import java.util.Optional;

/** Reply language. Swedish is the default; English is the catalog's canonical language. */
public enum Language {
    SV,
    EN;

    /** "sv" or "en", ignoring letter case; empty for anything else. */
    public static Optional<Language> parse(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return switch (code.strip().toLowerCase(Locale.ROOT)) {
            case "sv" -> Optional.of(SV);
            case "en" -> Optional.of(EN);
            default -> Optional.empty();
        };
    }

    /** Groups digits as Swedish (146 028 325) or English (146,028,325) text does. */
    public String number(long value) {
        String grouped = String.format(Locale.ROOT, "%,d", value);
        return this == SV ? grouped.replace(',', ' ') : grouped;
    }
}
