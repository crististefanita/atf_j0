package atf.reporting.standalone.publication;

import atf.reporting.extent.publication.InlinePngPayloadStore;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ReportingStandaloneInlinePayloadTest {
    @TempDir Path directory;
    @Test void restoresExactUnicodeBoundariesDuplicatesAndShortPayloads() throws Exception {
        String payload = "Ab/+=019".repeat(2500);
        String input = "♠" + "x".repeat(8180) + "data:image/png;base64," + payload + "\">tail"
                + "data:image/png;base64," + payload + "' end data:image/png;base64,AQID; ✓";
        Path source = directory.resolve("source.html"); Files.writeString(source, input);
        try (var captured = InlinePngPayloadStore.capture(source)) {
            assertTrue(captured.compactHtml().length() < input.length() / 2);
            var output = new StringWriter(); captured.writeRestored(captured.compactHtml(), output);
            assertEquals(input, output.toString());
        }
        assertNoSpool(); assertEquals(input, Files.readString(source));
    }
    @Test void writerFailureRetainsItsCauseAndCleansOwnedStorage() throws Exception {
        Path source = directory.resolve("source.html");
        Files.writeString(source, "data:image/png;base64," + "A".repeat(20000));
        var original = new IOException("Controlled writer failure");
        try (var captured = InlinePngPayloadStore.capture(source)) {
            var writer = new Writer() {
                public void write(char[] text, int offset, int count) throws IOException { throw original; }
                public void flush() { }
                public void close() { }
            };
            assertSame(original, assertThrows(IOException.class, () -> captured.writeRestored(captured.compactHtml(), writer)));
        }
        assertNoSpool();
    }
    @Test void truncatedPrivateStorageCannotBecomeASuccessfulArtifact() throws Exception {
        Path source = directory.resolve("source.html");
        Files.writeString(source, "data:image/png;base64," + "A".repeat(20000));
        try (var captured = InlinePngPayloadStore.capture(source)) {
            Path payload; try (var files = Files.list(directory)) { payload = files.filter(p -> p.getFileName().toString().startsWith(".atf-png-payload-")).findFirst().orElseThrow(); }
            try (var channel = java.nio.channels.FileChannel.open(payload, StandardOpenOption.WRITE)) { channel.truncate(0); }
            assertThrows(EOFException.class, () -> captured.writeRestored(captured.compactHtml(), new StringWriter()));
        }
        assertNoSpool();
    }
    @Test void textWithoutPayloadsAndPartialPrefixesRemainUnchanged() throws Exception {
        for (String text : List.of("", "d", "ddata:image/png;base64,AA=", "data:image/pn", "data:image/png;base64,", "♣ plain text")) {
            Path source = directory.resolve("source.html"); Files.writeString(source, text);
            try (var captured = InlinePngPayloadStore.capture(source)) {
                var restored = new StringWriter(); captured.writeRestored(captured.compactHtml(), restored);
                assertEquals(text, restored.toString());
            }
        }
        assertNoSpool();
    }
    private void assertNoSpool() throws Exception {
        try (var files = Files.list(directory)) { assertTrue(files.noneMatch(p -> p.getFileName().toString().startsWith(".atf-png-payload-"))); }
    }
}
