package atf.reporting.support.publication;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.HexFormat;
import java.util.regex.Pattern;

/** Independent artifact oracle: replace inline media with their streamed fingerprints, preserving their original DOM owners. */
public final class VolumeEvidenceSnapshot {
    public static final String PREFIX = "data:image/png;base64,";
    public static final String MARKER = "PNG-SHA256-";
    private static final Pattern PAYLOAD = Pattern.compile("[A-Za-z0-9+/=]+");
    private VolumeEvidenceSnapshot() { }
    public static String read(Path file) throws IOException {
        var shell = new StringBuilder();
        String carry = "";
        MessageDigest digest = null;
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            char[] buffer = new char[32_768];
            for (int size; (size = reader.read(buffer)) >= 0;) {
                String chunk = carry + new String(buffer, 0, size); carry = "";
                int cursor = 0;
                while (cursor < chunk.length()) {
                    if (digest == null) {
                        int start = chunk.indexOf(PREFIX, cursor);
                        if (start < 0) {
                            int boundary = Math.max(cursor, chunk.length() - PREFIX.length() + 1);
                            shell.append(chunk, cursor, boundary); carry = chunk.substring(boundary); break;
                        }
                        shell.append(chunk, cursor, start + PREFIX.length());
                        cursor = start + PREFIX.length(); digest = digest();
                    }
                    var payload = PAYLOAD.matcher(chunk).region(cursor, chunk.length());
                    int end = payload.lookingAt() ? payload.end() : cursor;
                    digest.update(chunk.substring(cursor, end).getBytes(StandardCharsets.US_ASCII));
                    cursor = end;
                    if (cursor == chunk.length()) break;
                    shell.append(MARKER).append(HexFormat.of().formatHex(digest.digest())); digest = null;
                }
            }
        }
        if (digest != null) shell.append(MARKER).append(HexFormat.of().formatHex(digest.digest()));
        return shell.append(carry).toString();
    }
    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException absent) { throw new IllegalStateException(absent); }
    }
}
