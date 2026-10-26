package atf.reporting.artifact.contract;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ReportArtifactContractGroupsTest {
    @Test
    void keepsSemanticContractGroupsIndependent() {
        var theme = new LinkedHashMap<String, Boolean>();
        var evidence = new LinkedHashMap<String, Boolean>();

        ReportArtifactThemeContracts.evaluate("<html>", theme);
        ReportArtifactEvidenceContracts.evaluate("<html>", evidence);

        assertTrue(theme.containsKey("ATF dark theme"));
        assertTrue(theme.containsKey("category status palette"));
        assertFalse(theme.containsKey("overview metadata"));
        assertTrue(evidence.containsKey("request-validation roles"));
        assertTrue(evidence.containsKey("failure evidence projection"));
        assertFalse(evidence.containsKey("ATF dark theme"));
    }

    @Test
    void facadePreservesEveryNamedContractWithoutDuplicateKeys() {
        var checks = ReportArtifactContractChecks.evaluate("<!doctype html><html></html>");

        assertEquals(checks.size(), new HashSet<>(checks.keySet()).size());
        assertTrue(checks.containsKey("scenario-only tags"));
        assertTrue(checks.containsKey("category selection projection"));
        assertTrue(checks.containsKey("runtime bootstrap placement"));
        assertTrue(checks.containsKey("result markers"));
    }
}
