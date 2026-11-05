package se.arbetsprov.ai.cli;

import java.net.URI;
import se.arbetsprov.ai.AskException;
import se.arbetsprov.ai.chat.Language;

/**
 * The command line, parsed by hand: a handful of options do not need a library.
 *
 * @param help    print usage and stop
 * @param setup   phase 1: check and install prerequisites
 * @param user    phase 2: chat as this user
 * @param service the REST service
 * @param ollama  the Ollama server
 * @param lang    starting reply language
 * @param json    print raw JSON instead of text
 */
public record CliOptions(boolean help, boolean setup, String user, URI service, URI ollama, Language lang, boolean json) {

    public static final URI DEFAULT_SERVICE = URI.create("http://localhost:8080");
    public static final URI DEFAULT_OLLAMA = URI.create("http://localhost:11434");

    public static final String USAGE = """
            Användning: ask --setup
                        ask --user <id> [--service <url>] [--ollama <url>] [--lang sv|en] [--json]
              --setup    Kontrollerar och installerar modell, tokenizer och Ollama (frågar Yes/No först)
              --user     Användar-id för besök (krävs för chatt), t.ex. alice
              --service  Tjänstens adress (standard: http://localhost:8080)
              --ollama   Ollamas adress (standard: http://localhost:11434)
              --lang     Svarsspråk från start, sv eller en (standard: sv)
              --json     Visa tjänstens svar som rå JSON
              --help     Visa denna hjälp
            I chatten: /sv eller /en byter språk, /exit avslutar.""";

    /**
     * @throws AskException with {@link AskException#USAGE} for anything that is not valid
     */
    public static CliOptions parse(String... args) {
        boolean help = false;
        boolean setup = false;
        boolean json = false;
        String user = null;
        URI service = DEFAULT_SERVICE;
        URI ollama = DEFAULT_OLLAMA;
        Language lang = Language.SV;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--help", "-h" -> help = true;
                case "--setup" -> setup = true;
                case "--json" -> json = true;
                case "--user" -> user = value(args, ++i, arg);
                case "--service" -> service = uri(value(args, ++i, arg), arg);
                case "--ollama" -> ollama = uri(value(args, ++i, arg), arg);
                case "--lang" -> {
                    String code = value(args, ++i, arg);
                    lang = Language.parse(code).orElseThrow(() -> usage("Språket måste vara sv eller en, inte " + code + "."));
                }
                default -> throw usage("Okänt alternativ: " + arg + ".");
            }
        }

        if (help) {
            return new CliOptions(true, setup, user, service, ollama, lang, json);
        }
        if (setup && user != null) {
            throw usage("Välj antingen --setup eller --user.");
        }
        if (!setup && (user == null || user.isBlank())) {
            throw new AskException(AskException.USAGE,
                    "Fel: --user saknas. Besök sparas per användare, ange t.ex. --user alice.");
        }
        return new CliOptions(false, setup, user == null ? null : user.strip(), service, ollama, lang, json);
    }

    private static String value(String[] args, int i, String option) {
        if (i >= args.length || args[i].startsWith("--")) {
            throw usage(option + " behöver ett värde.");
        }
        return args[i];
    }

    private static URI uri(String text, String option) {
        try {
            URI uri = URI.create(text);
            if (!uri.isAbsolute()) {
                throw new IllegalArgumentException();
            }
            return uri;
        } catch (IllegalArgumentException e) {
            throw usage(option + " måste vara en adress som http://localhost:8080, inte " + text + ".");
        }
    }

    private static AskException usage(String message) {
        return new AskException(AskException.USAGE, "Fel: " + message + "\n" + USAGE);
    }
}
