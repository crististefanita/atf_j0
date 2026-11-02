package atf.reporting.extent.publication;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.regex.Pattern;

/** Internal publication storage: opaque PNG payloads stay on disk while the HTML shell is transformed. */
public final class InlinePngPayloadStore implements AutoCloseable {
    private static final String PREFIX = "data:image/png;base64,";
    private static final int BUFFER_SIZE = 8192;
    private static final int INLINE_THRESHOLD = 4096;
    private record Slice(long offset, long length) { }
    private final Path parent;
    private final String nonce = "ATF" + UUID.randomUUID().toString().replace("-", "") + "X";
    private final Map<String, Slice> slots = new LinkedHashMap<>();
    private final Map<String, String> identities = new HashMap<>();
    private Path file;
    private RandomAccessFile spool;
    private String compact;

    private InlinePngPayloadStore(Path parent) { this.parent = parent; }

    public static InlinePngPayloadStore capture(Path html) throws IOException {
        var result = new InlinePngPayloadStore(html.toAbsolutePath().getParent());
        try { result.compact = result.read(html); return result; }
        catch (IOException | RuntimeException | Error failure) { try (result) { throw failure; } }
    }

    public String compactHtml() { return compact; }

    private String read(Path html) throws IOException {
        var text = new StringBuilder();
        try (var input = new Characters(Files.newBufferedReader(html, StandardCharsets.UTF_8))) {
            for (int current; (current = input.read()) >= 0;) {
                if (current != PREFIX.charAt(0)) { text.append((char) current); continue; }
                int matched = 1;
                while (matched < PREFIX.length()) {
                    int next = input.read();
                    if (next != PREFIX.charAt(matched)) { input.unread(next); break; }
                    matched++;
                }
                text.append(PREFIX, 0, matched);
                if (matched == PREFIX.length()) text.append(payload(input));
            }
        }
        return text.toString();
    }

    private String payload(Characters input) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int size = 0, value = -1;
        while (size < INLINE_THRESHOLD && base64(value = input.read())) buffer[size++] = (byte) value;
        if (size < INLINE_THRESHOLD) {
            input.unread(value);
            return new String(buffer, 0, size, StandardCharsets.US_ASCII);
        }
        if (spool == null) {
            file = Files.createTempFile(parent, ".atf-png-payload-", ".tmp");
            spool = new RandomAccessFile(file.toFile(), "rw");
        }
        long start = spool.length();
        spool.seek(start);
        MessageDigest digest;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException unavailable) { throw new IllegalStateException("SHA-256 is required", unavailable); }
        do {
            if (size == buffer.length) {
                spool.write(buffer, 0, size); digest.update(buffer, 0, size); size = 0;
            }
            value = input.read();
            if (base64(value)) buffer[size++] = (byte) value;
        } while (base64(value));
        input.unread(value);
        if (size > 0) { spool.write(buffer, 0, size); digest.update(buffer, 0, size); }
        long length = spool.getFilePointer() - start;
        String identity = HexFormat.of().formatHex(digest.digest());
        String existing = identities.get(identity);
        if (existing != null) { spool.setLength(start); return existing; }
        String token = nonce + slots.size() + "Z";
        identities.put(identity, token); slots.put(token, new Slice(start, length));
        return token;
    }

    public void writeRestored(String html, Writer output) throws IOException {
        var matcher = Pattern.compile(Pattern.quote(nonce) + "[0-9]+Z").matcher(html);
        int previous = 0;
        byte[] bytes = new byte[BUFFER_SIZE]; char[] characters = new char[BUFFER_SIZE];
        while (matcher.find()) {
            output.write(html, previous, matcher.start() - previous);
            Slice slice = slots.get(matcher.group());
            if (slice == null) throw new IOException("Unknown inline payload slot");
            spool.seek(slice.offset());
            long remaining = slice.length();
            while (remaining > 0) {
                int read = spool.read(bytes, 0, (int) Math.min(bytes.length, remaining));
                if (read < 0) throw new EOFException("Incomplete inline payload staging");
                for (int i = 0; i < read; i++) characters[i] = (char) (bytes[i] & 0xff);
                output.write(characters, 0, read); remaining -= read;
            }
            previous = matcher.end();
        }
        output.write(html, previous, html.length() - previous);
    }

    @Override public void close() throws IOException {
        try { if (spool != null) spool.close(); }
        finally { if (file != null) Files.deleteIfExists(file); }
    }
    private static boolean base64(int value) {
        return value >= 'A' && value <= 'Z' || value >= 'a' && value <= 'z'
                || value >= '0' && value <= '9' || value == '+' || value == '/' || value == '=';
    }
    private static final class Characters implements AutoCloseable {
        private final Reader reader;
        private final char[] buffer = new char[BUFFER_SIZE];
        private int position, length, pushed = -1;
        private Characters(Reader reader) { this.reader = reader; }
        int read() throws IOException {
            if (length < 0) return -1;
            if (pushed >= 0) { int value = pushed; pushed = -1; return value; }
            if (position == length) { length = reader.read(buffer); position = 0; if (length < 0) return -1; }
            return buffer[position++];
        }
        void unread(int value) { if (value >= 0) pushed = value; }
        @Override public void close() throws IOException { reader.close(); }
    }
}
