package atf.core;
import atf.testkit.AtfUnitTest;

import atf.core.reporting.duration.AtfDurationParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest

class AtfDurationParserTest {
    @Test
    void parsesAllPublishedDurationFormsAndPreservesZero() {
        assertEquals(0, AtfDurationParser.parse("0 ms").orElseThrow());
        assertEquals(301, AtfDurationParser.parse("0.301 s").orElseThrow());
        assertEquals(62_003, AtfDurationParser.parse("01:02:003").orElseThrow());
        assertEquals(3_723_004, AtfDurationParser.parse("01:02:03:004").orElseThrow());
    }

    @Test
    void absentAndMalformedValuesAreExplicitlyMissing() {
        assertTrue(AtfDurationParser.parse(null).isEmpty());
        assertTrue(AtfDurationParser.parse("—").isEmpty());
        assertTrue(AtfDurationParser.parse("not-a-duration").isEmpty());
        assertTrue(AtfDurationParser.parse("-1 ms").isEmpty());
    }
}
