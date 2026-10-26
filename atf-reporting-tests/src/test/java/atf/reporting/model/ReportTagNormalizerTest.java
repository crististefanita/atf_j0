package atf.reporting.model;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@AtfUnitTest
class ReportTagNormalizerTest {
    @Test
    void trimsLowercasesDropsBlanksAndDeduplicatesCaseInsensitively() {
        assertEquals(List.of("api", "mock-contract", "ui"),
                ReportTagNormalizer.normalize(List.of(
                        " API ", "mock-contract", "API", "", "  ", "UI", "mock-contract")));
    }

    @Test
    void nullInputAndNullEntriesProduceAnEmptyOrStableCollection() {
        assertEquals(List.of(), ReportTagNormalizer.normalize(null));
        assertEquals(List.of("api"), ReportTagNormalizer.normalize(java.util.Arrays.asList(null, " API ")));
    }
}
