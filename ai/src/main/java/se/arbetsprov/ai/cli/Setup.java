package se.arbetsprov.ai.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import se.arbetsprov.ai.AskException;

/**
 * Phase 1, {@code ask --setup}: checks the model, the tokenizer, Ollama and the Ollama model in
 * turn, and asks {@code Installera? (Yes/No)} before installing anything that is missing. Only
 * "Yes", "yes", "No" and "no" are accepted. Verified files are recorded in the marker.
 */
public final class Setup {

    private static final Set<String> YES = Set.of("Yes", "yes");
    private static final Set<String> NO = Set.of("No", "no");

    private final AskContext context;
    private final BufferedReader in;
    private final PrintStream out;
    private boolean complete = true;

    public Setup(AskContext context, BufferedReader in, PrintStream out) {
        this.context = context;
        this.in = in;
        this.out = out;
    }

    /** Returns 0; fails with exit code 3 if a download does not match its checksum. */
    public int run() {
        out.println("Kontrollerar förutsättningar för ask (" + context.cacheDir() + ")");
        var verified = new LinkedHashMap<String, String>();
        for (ModelFile file : context.files()) {
            if (checkFile(file)) {
                verified.put(file.name(), file.sha256());
            }
        }
        Prerequisites.writeMarker(context, verified);

        if (checkOllama()) {
            checkOllamaModel();
        }

        out.println(complete
                ? "Klart. Starta chatten med: ask --user <namn>"
                : "Klart, men något saknas fortfarande (se ovan). Kör ask --setup igen när du vill.");
        return 0;
    }

    private boolean checkFile(ModelFile file) {
        Path target = context.modelsDir().resolve(file.name());
        if (Files.isRegularFile(target) && file.sha256().equals(sha256(target))) {
            out.println("[ok] " + file.name());
            return true;
        }
        out.println("[saknas] " + file.name() + " (" + megabytes(file.size()) + ") från " + file.url());
        if (!askYes()) {
            out.println("Hoppar över. Ladda ner manuellt: " + file.url() + " till " + context.modelsDir());
            complete = false;
            return false;
        }
        download(file, target);
        out.println("[ok] " + file.name() + " hämtad och kontrollerad");
        return true;
    }

    private boolean checkOllama() {
        if (context.commands().run(List.of("ollama", "--version")).ok()) {
            out.println("[ok] Ollama");
            return true;
        }
        switch (context.os()) {
            case LINUX -> {
                out.println("[saknas] Ollama. Installera med: curl -fsSL https://ollama.com/install.sh | sh");
                complete = false;
                return false;
            }
            case WINDOWS, MAC -> {
                List<String> install = context.os() == Os.WINDOWS
                        ? List.of("winget", "install", "Ollama.Ollama", "--accept-source-agreements",
                        "--accept-package-agreements")
                        : List.of("brew", "install", "ollama");
                out.println("[saknas] Ollama (installeras med: " + String.join(" ", install) + ")");
                if (!askYes()) {
                    out.println("Hoppar över. Installera manuellt: " + String.join(" ", install));
                    complete = false;
                    return false;
                }
                context.commands().runVisible(install);
                if (!context.commands().run(List.of("ollama", "--version")).ok()) {
                    out.println("Ollama är installerat men syns inte ännu. Öppna en ny terminal och kör ask --setup igen.");
                    complete = false;
                    return false;
                }
                out.println("[ok] Ollama installerat");
                return true;
            }
        }
        return false;
    }

    private void checkOllamaModel() {
        String model = context.ollamaModel();
        var list = context.commands().run(List.of("ollama", "list"));
        if (list.ok() && list.output().contains(model)) {
            out.println("[ok] " + model);
            return;
        }
        out.println("[saknas] Ollama-modellen " + model + " (cirka 1 GB)");
        if (!askYes()) {
            out.println("Hoppar över. Kör manuellt: ollama pull " + model);
            complete = false;
            return;
        }
        if (context.commands().runVisible(List.of("ollama", "pull", model)) != 0) {
            out.println("Kunde inte hämta " + model + ". Kör manuellt: ollama pull " + model);
            complete = false;
            return;
        }
        out.println("[ok] " + model + " hämtad");
    }

    /** Asks until the answer is exactly Yes, yes, No or no; end of input counts as No. */
    private boolean askYes() {
        while (true) {
            out.print("Installera? (Yes/No) ");
            out.flush();
            String answer;
            try {
                answer = in.readLine();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            if (answer == null) {
                out.println();
                return false;
            }
            answer = answer.strip();
            if (YES.contains(answer)) {
                return true;
            }
            if (NO.contains(answer)) {
                return false;
            }
            out.println("Svara Yes eller No.");
        }
    }

    /**
     * Streams the file to disk (it is too large to hold in memory, so this goes around the core
     * pipeline) and keeps it only if its SHA-256 matches.
     */
    private void download(ModelFile file, Path target) {
        try {
            Files.createDirectories(target.getParent());
            Path partial = target.resolveSibling(file.name() + ".part");
            var client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(20))
                    .build();
            var response = client.send(HttpRequest.newBuilder(file.url()).GET().build(),
                    HttpResponse.BodyHandlers.ofFile(partial));
            if (response.statusCode() != 200) {
                Files.deleteIfExists(partial);
                throw new AskException(AskException.MISSING_PREREQUISITE,
                        "Kunde inte hämta " + file.name() + ": HTTP " + response.statusCode());
            }
            if (!file.sha256().equals(sha256(partial))) {
                Files.deleteIfExists(partial);
                throw new AskException(AskException.MISSING_PREREQUISITE,
                        "Kontrollsumman stämmer inte för " + file.name() + ". Filen är borttagen; försök igen.");
            }
            Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AskException(AskException.UNREACHABLE, "Kunde inte hämta " + file.name() + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AskException(AskException.UNREACHABLE, "Avbruten nedladdning av " + file.name(), e);
        }
    }

    static String sha256(Path path) {
        try (InputStream in = new DigestInputStream(Files.newInputStream(path), MessageDigest.getInstance("SHA-256"))) {
            in.transferTo(OutputStreamSink.INSTANCE);
            return HexFormat.of().formatHex(((DigestInputStream) in).getMessageDigest().digest());
        } catch (IOException e) {
            return "";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String megabytes(long bytes) {
        return Math.round(bytes / 1_000_000.0) + " MB";
    }

    /** Discards bytes; the digest is computed while reading. */
    private static final class OutputStreamSink extends java.io.OutputStream {
        static final OutputStreamSink INSTANCE = new OutputStreamSink();

        @Override
        public void write(int b) {
        }

        @Override
        public void write(byte[] b, int off, int len) {
        }
    }

    /** For tests: the marker contents after a run. */
    Map<String, Map<String, Object>> marker() {
        return Prerequisites.readMarker(context);
    }
}
