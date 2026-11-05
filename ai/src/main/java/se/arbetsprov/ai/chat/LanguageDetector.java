package se.arbetsprov.ai.chat;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Tells Swedish from English by counting common words, with å, ä and ö deciding a tie. Small,
 * predictable and local: a 1.5B model guesses the language less reliably than this, and is better
 * used for the parameters alone.
 *
 * <p>Words both languages use ("i", "in", "de") are left out, and country names do not count.
 */
final class LanguageDetector {

    private static final Set<String> SWEDISH = Set.of(
            "och", "jag", "är", "vad", "vilka", "vilket", "vilken", "det", "den", "som", "på", "för", "till",
            "från", "med", "har", "hade", "varit", "mina", "min", "mitt", "mig", "visa", "lista", "ta", "bort",
            "ändra", "markera", "länder", "länderna", "landet", "land", "största", "minsta", "flest", "minst",
            "huvudstaden", "huvudstad", "hur", "många", "bor", "besökt", "besök", "anteckning", "efter",
            "sorterade", "alla", "tre", "fem", "tio");

    private static final Set<String> ENGLISH = Set.of(
            "the", "and", "what", "which", "is", "are", "show", "list", "my", "of", "to", "for", "with", "have",
            "has", "been", "me", "countries", "country", "largest", "smallest", "biggest", "most", "fewest",
            "capital", "how", "many", "people", "live", "visited", "visit", "note", "remove", "change", "mark",
            "sorted", "all", "where", "tell", "about", "three", "five", "ten");

    private LanguageDetector() {
    }

    /** The language of {@code text}, or empty when it cannot tell (keep the current language). */
    static Optional<Language> detect(String text) {
        int swedish = 0;
        int english = 0;
        for (String word : text.toLowerCase(Locale.ROOT).split("[^\\p{L}]+")) {
            if (SWEDISH.contains(word)) {
                swedish++;
            }
            if (ENGLISH.contains(word)) {
                english++;
            }
        }
        if (swedish != english) {
            return Optional.of(swedish > english ? Language.SV : Language.EN);
        }
        return text.matches("(?s).*[åäöÅÄÖ].*") ? Optional.of(Language.SV) : Optional.empty();
    }
}
