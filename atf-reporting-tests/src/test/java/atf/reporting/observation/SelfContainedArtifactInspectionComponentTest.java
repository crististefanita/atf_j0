package atf.reporting.observation;

import atf.reporting.artifact.ReportArtifactInspector;
import atf.reporting.html.contract.HtmlReportStaticContract;
import atf.reporting.support.composition.SelfContainedAuditFixture;
import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.ArtifactProfileFixture;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Real published owners plus independent external-evidence mutants, equally executable against source and a physical JAR. */
@AtfComponentTest
class SelfContainedArtifactInspectionComponentTest {
    @TempDir Path directory;

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void publicInspectorRefusesExternalEvidenceWithoutRejectingLiteralProvenance(boolean fallback) throws Exception {
        Path file = directory.resolve("autonomous-" + fallback + ".html");
        var profile = ArtifactProfileFixture.publish(file, fallback, true, false);
        String original = Files.readString(file);
        String png = "data:image/png;base64," + SelfContainedAuditFixture.png(0);
        String permitted = "<pre>C:\\work\\capture.xml file:///original/capture.xml &lt;img src='missing.png'&gt;</pre>"
                + "<a href='https://example.org/guide'>Documentation</a>"
                + "<script>var literal = \"<img src='file:///not-a-resource.png'>\";</script>"
                + "<a id='atf-screenshot-case' data-atf-screenshot-ref='case' href='" + png + "'>Frame</a>"
                + "<a data-atf-screenshot-ref='case' href='#atf-screenshot-case'>Same complete frame</a>";
        Files.writeString(file, append(original, permitted));
        var nominal = ReportArtifactInspector.inspect(file, profile);
        assertTrue(nominal.isCompliant(), () -> nominal.violations() + " " + nominal.diagnostics());
        if (fallback) assertTrue(HtmlReportStaticContract.evaluate(Files.readString(file)).satisfies("no project absolute paths"));
        List<String> mutations = List.of(
                "<a href='file:///missing/receipt.xml'>Receipt</a>",
                "<img src='file:///missing/frame.png'>", "<img src='https://example.org/frame.png'>",
                "<img srcset='" + png + " 1x, missing-frame.png 2x'>",
                "<style>@import 'missing-theme.css';</style>",
                "<div style='background:url(missing-frame.png)'>Frame</div>",
                "<svg><image href='missing-frame.png'/></svg>",
                "<object data='missing-record.xml'></object>",
                "<a download href='https://example.org/evidence'>Download evidence</a>",
                "<a href='receipt.xml'>Read receipt</a>",
                "<div data-atf-node-role='observation'><a href='https://example.org/evidence'>Artifact</a></div>",
                "<iframe srcdoc=\"&lt;img src='missing-frame.png'&gt;\"></iframe>",
                "<a data-atf-screenshot-ref='missing' href='#atf-screenshot-missing'>Missing canonical PNG</a>");
        for (int index = 0; index < mutations.size(); index++) {
            Path mutant = directory.resolve("external-" + fallback + "-" + index + ".html");
            Files.writeString(mutant, append(original, mutations.get(index)));
            var inspected = ReportArtifactInspector.inspect(mutant, profile);
            assertFalse(inspected.isCompliant(), mutations.get(index));
            assertFalse(inspected.satisfies("autonomous artifact"), mutations.get(index));
            assertFalse(ReportArtifactInspector.inspect(mutant).satisfies("autonomous artifact"));
            var diagnostics = inspected.diagnostics().stream().filter(d -> d.check().equals("autonomous artifact")).toList();
            assertEquals(1, diagnostics.size(), "One diagnostic per check retains the inspection result contract");
            assertFalse(diagnostics.getFirst().passed());
            assertTrue(diagnostics.getFirst().evidence().contains("["), "The attribute and DOM owner are actionable");
            assertFalse(diagnostics.getFirst().evidence().isBlank());
            if (fallback) assertFalse(HtmlReportStaticContract.evaluate(Files.readString(mutant)).satisfies("no project absolute paths"));
        }
        assertTrue(ReportArtifactInspector.inspect(file, profile).isCompliant(), "Mutants never overwrite the nominal report");
        System.out.println("AUTONOMOUS_PROFILE_REPORT=" + file);
    }

    private static String append(String original, String addition) {
        int end = original.lastIndexOf("</body>"); assertTrue(end > 0);
        return original.substring(0, end) + addition + original.substring(end);
    }
}
