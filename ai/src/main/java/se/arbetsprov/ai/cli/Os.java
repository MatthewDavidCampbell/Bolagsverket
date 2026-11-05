package se.arbetsprov.ai.cli;

import java.util.Locale;

/** The operating system, which decides how Ollama is installed. */
public enum Os {
    WINDOWS,
    MAC,
    LINUX;

    public static Os current() {
        String name = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (name.contains("win")) {
            return WINDOWS;
        }
        return name.contains("mac") ? MAC : LINUX;
    }
}
