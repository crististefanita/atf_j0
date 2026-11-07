package atf.reporting.browser.process;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.regex.Pattern;

/** Complete append-only frames advance the cursor without rescans or replacing an open Windows file. */
public final class BrowserPhaseChannel implements AutoCloseable {
    private static final String CURSOR = "phase-current";
    private static final String INITIAL = "phase-00000000-SETUP";
    private static final Pattern EVENT = Pattern.compile("phase-\\d{8}-(SETUP|VERIFY|CLEANUP|DONE|FAILED)");

    private final Path cursor;
    private final ByteBuffer buffer = ByteBuffer.allocate(1024);
    private final StringBuilder pending = new StringBuilder();
    private FileChannel input;
    private String current = INITIAL;
    private long position;

    public BrowserPhaseChannel(Path directory) { cursor = directory.resolve(CURSOR); }

    public static void publish(Path directory, String event, long timestamp) throws IOException {
        requireEvent(event);
        Files.writeString(directory.resolve(event), Long.toString(timestamp), StandardOpenOption.CREATE_NEW);
        Files.writeString(directory.resolve(CURSOR), event + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public String current() throws IOException {
        if (input == null) {
            if (!Files.exists(cursor)) return current;
            input = FileChannel.open(cursor, StandardOpenOption.READ);
        }
        long limit = input.size();
        if (limit < position) throw new IOException("Browser phase channel was truncated");
        while (position < limit) {
            buffer.clear(); buffer.limit((int) Math.min(buffer.capacity(), limit - position));
            int count = input.read(buffer, position);
            if (count <= 0) break;
            position += count; buffer.flip();
            while (buffer.hasRemaining()) {
                char value = (char) (buffer.get() & 255);
                if (value == '\n') { String event = pending.toString(); requireEvent(event); current = event; pending.setLength(0); }
                else {
                    if (pending.length() == 64) throw new IOException("Browser phase frame exceeds its protocol bound");
                    pending.append(value);
                }
            }
        }
        return current;
    }

    @Override public void close() throws IOException { if (input != null) input.close(); }

    private static void requireEvent(String event) throws IOException {
        if (!EVENT.matcher(event).matches()) throw new IOException("Invalid browser phase cursor: " + event);
    }
}
