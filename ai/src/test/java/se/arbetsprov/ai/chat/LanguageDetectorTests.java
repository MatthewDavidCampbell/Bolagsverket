package se.arbetsprov.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Replies follow the question's language, detected locally rather than by the LLM.
 */
@DisplayName("Language detector")
class LanguageDetectorTests {

    @Test
    void swedishQuestions_areSwedish() {
        // Given Swedish questions, including ones that name countries
        // When detected
        // Then each is Swedish
        assertThat(LanguageDetector.detect("vad är huvudstaden i Norge?")).contains(Language.SV);
        assertThat(LanguageDetector.detect("jag har varit i Italien")).contains(Language.SV);
        assertThat(LanguageDetector.detect("lista de fem största länderna i Europa")).contains(Language.SV);
    }

    @Test
    void englishQuestions_areEnglish() {
        // Given English questions
        // When detected
        // Then each is English
        assertThat(LanguageDetector.detect("show the three smallest countries in Oceania")).contains(Language.EN);
        assertThat(LanguageDetector.detect("What is the capital of Norway?")).contains(Language.EN);
    }

    @Test
    void noClue_usesSwedishLettersOrGivesUp() {
        // Given text with no known words
        // When detected
        // Then å, ä or ö mean Swedish, and otherwise nothing is decided (the current language stays)
        assertThat(LanguageDetector.detect("Göteborg")).contains(Language.SV);
        assertThat(LanguageDetector.detect("Norge")).isEmpty();
    }
}
