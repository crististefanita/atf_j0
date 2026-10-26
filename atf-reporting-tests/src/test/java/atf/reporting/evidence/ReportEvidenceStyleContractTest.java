package atf.reporting.evidence;
import atf.testkit.AtfUnitTest;

import atf.reporting.support.TestReportPaths;
import atf.reporting.theme.AtfReportTheme;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@AtfUnitTest
class ReportEvidenceStyleContractTest {
    @Test
    void sharedShellContractCoversBothSemanticFamilies() {
        String css = AtfReportTheme.css();
        String keyboardContract = "summary.setAttribute('aria-expanded','false');details.open?'true':'false'";
        String ui = "<html><style>" + css + "</style><script>" + keyboardContract + "</script><body>"
                + "<details class=\"atf-ui-action-sequence\">"
                + "<details class=\"atf-ui-observations\"><a class=\"atf-ui-action-screenshot-link\"></a>"
                + "<table class=\"helm-evidence-table helm-validation-table\"></table>";
        String api = "<html><style>" + css + "</style><script>" + keyboardContract + "</script><body>"
                + "<details class=\"request-evidence\">"
                + "<details class=\"response-evidence\"><details class=\"execution-evidence\">"
                + "<table class=\"helm-evidence-table helm-validation-table\"></table>";

        Path directory = TestReportPaths.report("style-contract-ui.html").getParent();
        try {
            Files.createDirectories(directory);
            Path uiPath = directory.resolve("style-contract-ui.html");
            Path apiPath = directory.resolve("style-contract-api.html");
            Files.writeString(uiPath, ui);
            Files.writeString(apiPath, api);
            var comparison = ReportEvidenceStyleContract.compare(uiPath, apiPath);
            assertTrue(comparison.compliant(), () -> "UI=" + comparison.ui().checks()
                    + " API=" + comparison.api().checks()
                    + " cross=" + comparison.crossChecks());

            Files.writeString(apiPath, api.replace("--atf-evidence-status-column-width:52px",
                    "--atf-evidence-status-column-width:60px"));
            var drift = ReportEvidenceStyleContract.compare(uiPath, apiPath);
            assertFalse(drift.compliant());
            assertFalse(drift.crossChecks().get("shared token --atf-evidence-status-column-width"));

            String noApiEvidence = api.replace("<details class=\"request-evidence\">", "")
                    .replace("<details class=\"response-evidence\">", "")
                    .replace("<details class=\"execution-evidence\">", "");
            assertFalse(ReportEvidenceStyleContract.inspectHtml(apiPath, noApiEvidence, false).compliant(),
                    "CSS selectors alone must not satisfy the API evidence contract");
            assertFalse(ReportEvidenceStyleContract.inspectHtml(uiPath,
                    ui.replace("<details class=\"atf-ui-action-sequence\">", ""), true).compliant(),
                    "CSS selectors alone must not satisfy the UI evidence contract");
        } catch (java.io.IOException error) {
            throw new AssertionError(error);
        }
    }
}
