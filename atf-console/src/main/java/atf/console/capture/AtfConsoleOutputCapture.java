package atf.console.capture;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Scoped console observation for synchronous launcher proofs; consumers never own process streams. */
public final class AtfConsoleOutputCapture {
    private AtfConsoleOutputCapture() { }

    /**
     * Serializes cooperating captures and restores stdout even when the action fails.
     * The observer sees each reporter println at emission time; it must not write to stdout.
     * Unrelated threads must not produce output during an observation.
     */
    public static synchronized <T> Observation<T> capture(Callable<T> action, Consumer<String> lineObserver) throws Exception {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(lineObserver, "lineObserver");
        PrintStream original = System.out;
        var bytes = new ByteArrayOutputStream();
        try (var capture = new PrintStream(bytes, true, StandardCharsets.UTF_8) {
            @Override public void println(String line) {
                lineObserver.accept(line);
                super.println(line);
            }
        }) {
            System.setOut(capture);
            T result = action.call();
            return new Observation<>(result, bytes.toString(StandardCharsets.UTF_8));
        } finally {
            System.setOut(original);
        }
    }

    public record Observation<T>(T result, String output) { }
}
