package se.arbetsprov.ai;

/**
 * A failure that ends the CLI with an exit code. Thrown wherever it happens and caught only in
 * the CLI's main method, so problems fail fast instead of being worked around.
 */
public class AskException extends RuntimeException {

    /** Wrong or missing command-line options. */
    public static final int USAGE = 2;
    /** Model, tokenizer, Ollama or the Ollama model is missing: run {@code ask --setup}. */
    public static final int MISSING_PREREQUISITE = 3;
    /** The service or Ollama does not answer. */
    public static final int UNREACHABLE = 4;

    private final int exitCode;

    public AskException(int exitCode, String message) {
        super(message);
        this.exitCode = exitCode;
    }

    public AskException(int exitCode, String message, Throwable cause) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public int exitCode() {
        return exitCode;
    }
}
