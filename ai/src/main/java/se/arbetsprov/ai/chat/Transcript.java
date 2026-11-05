package se.arbetsprov.ai.chat;

import java.io.PrintStream;

/**
 * Prints the conversation with a prefix per speaker: {@code (chat) } for the CLI and
 * {@code (<user>) } for the user. When input is piped rather than typed, the user's line is
 * echoed after the prefix so the transcript still reads as a conversation.
 */
public final class Transcript {

    private final PrintStream out;
    private final String user;
    private final boolean echoInput;

    public Transcript(PrintStream out, String user, boolean echoInput) {
        this.out = out;
        this.user = user;
        this.echoInput = echoInput;
    }

    /** A line from the CLI. */
    public void chat(String line) {
        out.println("(chat) " + line);
    }

    /** Shows the user's prefix before they type. */
    public void prompt() {
        out.print("(" + user + ") ");
        out.flush();
    }

    /** Called with what the user typed; echoes it when input is not a terminal. */
    public void typed(String line) {
        if (echoInput) {
            out.println(line);
        }
    }

    /** A raw line, for --json output. */
    public void raw(String text) {
        out.println(text);
    }
}
