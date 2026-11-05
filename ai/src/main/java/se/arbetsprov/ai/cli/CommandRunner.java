package se.arbetsprov.ai.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Runs external commands (ollama, winget, brew). Replaced by a recording fake in tests. */
public interface CommandRunner {

    /**
     * @param exitCode the command's exit code; -1 when it could not be started (not installed)
     * @param output   what it printed
     */
    record Result(int exitCode, String output) {

        public boolean ok() {
            return exitCode == 0;
        }
    }

    /** Runs and captures the output. */
    Result run(List<String> command);

    /** Runs with output shown directly to the user, for long installs and downloads. */
    int runVisible(List<String> command);

    /** The real implementation, with {@link ProcessBuilder}. */
    static CommandRunner system() {
        return new CommandRunner() {
            @Override
            public Result run(List<String> command) {
                try {
                    var process = new ProcessBuilder(command).redirectErrorStream(true).start();
                    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    return new Result(process.waitFor(), output);
                } catch (IOException e) {
                    return new Result(-1, e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return new Result(-1, "interrupted");
                }
            }

            @Override
            public int runVisible(List<String> command) {
                try {
                    return new ProcessBuilder(command).inheritIO().start().waitFor();
                } catch (IOException e) {
                    return -1;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return -1;
                }
            }
        };
    }
}
