package se.arbetsprov.ai.chat;

/** The chat's fixed sentences in Swedish and English. */
final class Messages {

    private Messages() {
    }

    static String greeting(Language l) {
        return l == Language.SV ? "Vad vill du göra?" : "What would you like to do?";
    }

    static String notUnderstood(Language l) {
        return l == Language.SV ? "Jag förstod inte, försök igen." : "I did not understand, please try again.";
    }

    static String invalidValues(Language l, String detail) {
        return (l == Language.SV ? "Jag kunde inte använda värdena: " : "I could not use the values: ") + detail;
    }

    static String switched(Language l) {
        return l == Language.SV ? "Jag svarar på svenska." : "I will answer in English.";
    }

    static String countryLine(Language l, int number, String name, long population) {
        return number + ". " + name + " – " + l.number(population) + (l == Language.SV ? " invånare" : " people");
    }

    static String pageFooter(Language l, int page, int pages, long total) {
        return l == Language.SV
                ? "Sida " + (page + 1) + " av " + pages + ", " + l.number(total) + " länder totalt."
                : "Page " + (page + 1) + " of " + pages + ", " + l.number(total) + " countries in total.";
    }

    static String noCountries(Language l) {
        return l == Language.SV ? "Inga länder hittades." : "No countries found.";
    }

    static String country(Language l, String name, String official, String capital, String region,
                          String subregion, long population) {
        String where = subregion == null || subregion.isBlank() ? region : region + " / " + subregion;
        if (l == Language.SV) {
            return name + " (" + official + "): huvudstad " + (capital == null ? "saknas" : capital) + ", " + where
                    + ", " + l.number(population) + " invånare.";
        }
        return name + " (" + official + "): capital " + (capital == null ? "none" : capital) + ", " + where
                + ", " + l.number(population) + " people.";
    }

    static String visitLine(Language l, int number, String name, String code, String note) {
        return number + ". " + name + " (" + code + ")" + (note == null || note.isBlank() ? "" : " – " + note);
    }

    static String noVisits(Language l) {
        return l == Language.SV ? "Du har inga besökta länder än." : "You have no visited countries yet.";
    }

    static String visitCreated(Language l, String name) {
        return l == Language.SV ? name + " är markerat som besökt." : name + " is marked as visited.";
    }

    static String visitUpdated(Language l, String name) {
        return l == Language.SV ? "Anteckningen för " + name + " är ändrad." : "The note for " + name + " is updated.";
    }

    static String visitDeleted(Language l, String code) {
        return l == Language.SV ? code + " är inte längre markerat som besökt." : code + " is no longer marked as visited.";
    }
}
