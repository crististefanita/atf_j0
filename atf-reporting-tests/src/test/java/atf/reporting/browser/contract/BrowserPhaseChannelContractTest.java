package atf.reporting.browser.contract;

import atf.reporting.browser.process.BrowserPhaseChannel;
import atf.testkit.AtfComponentTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Framed publication preserves the journal and works while the Windows reader remains open. */
@AtfComponentTest
@Tag("report-browser")
class BrowserPhaseChannelContractTest {
    @TempDir Path directory;

    @Test void anUnstartedWorkerRemainsInTheBoundedSetupPhase() throws Exception {
        try (var channel = new BrowserPhaseChannel(directory)) { assertEquals("phase-00000000-SETUP", channel.current()); }
    }

    @Test void publicationPreservesEveryMarkerAndLeavesOnlyTheCompleteCursor() throws Exception {
        var events = List.of("phase-00000001-SETUP", "phase-00000002-VERIFY", "phase-00000003-CLEANUP", "phase-00000004-DONE");
        try (var channel = new BrowserPhaseChannel(directory)) {
            for (int index = 0; index < events.size(); index++) {
                BrowserPhaseChannel.publish(directory, events.get(index), index + 11);
                assertEquals(events.get(index), channel.current());
            }
        }
        for (int index = 0; index < events.size(); index++) assertEquals(Integer.toString(index + 11), Files.readString(directory.resolve(events.get(index))));
        try (var files = Files.list(directory)) { assertEquals(5, files.count(), "Four immutable events and one committed cursor; no pending publication"); }
    }

    @Test void readingTheCursorDoesNotEnumerateOrAcceptUncommittedJournalEntries() throws Exception {
        BrowserPhaseChannel.publish(directory, "phase-00000001-VERIFY", 11);
        Files.writeString(directory.resolve("phase-99999999-FAILED"), "uncommitted");
        Files.createDirectory(directory.resolve("phase-99999998-CLEANUP"));
        try (var channel = new BrowserPhaseChannel(directory)) { assertEquals("phase-00000001-VERIFY", channel.current()); }
    }

    @Test void aMalformedCursorFailsClosedInsteadOfInventingAPhase() throws Exception {
        Files.writeString(directory.resolve("phase-current"), "phase-00000002-UNKNOWN\n");
        try (var channel = new BrowserPhaseChannel(directory)) { assertThrows(IOException.class, channel::current); }
    }

    @Test void anIncompleteFrameCannotAdvanceThePhaseUntilItsCommitDelimiterArrives() throws Exception {
        BrowserPhaseChannel.publish(directory, "phase-00000001-VERIFY", 11);
        try (var channel = new BrowserPhaseChannel(directory)) {
            assertEquals("phase-00000001-VERIFY", channel.current());
            Path cursor = directory.resolve("phase-current");
            Files.writeString(cursor, "phase-00000002-CLEA", java.nio.file.StandardOpenOption.APPEND);
            assertEquals("phase-00000001-VERIFY", channel.current());
            Files.writeString(cursor, "NUP\n", java.nio.file.StandardOpenOption.APPEND);
            assertEquals("phase-00000002-CLEANUP", channel.current());
        }
    }

    @Test void invalidEventsCannotPublishFilesOrEscapeTheOwnedDirectory() throws Exception {
        for (String event : List.of("../outside", "phase-00000001-UNKNOWN", "phase-1-SETUP", ""))
            assertThrows(IOException.class, () -> BrowserPhaseChannel.publish(directory, event, 11));
        try (var files = Files.list(directory)) { assertEquals(0, files.count()); }
    }
}
