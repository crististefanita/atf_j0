package atf.reporting.support.composition;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.*;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.*;
import atf.reporting.model.timing.ReportPhaseTiming;
import atf.reporting.support.TestReportPaths;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import javax.imageio.ImageIO;

/** Controlled captured facts, not Android interaction or a business evaluator. */
public final class SelfContainedAuditFixture {
    public static final String CANARY = "SELF_CONTAINED_CREDENTIAL_CANARY";
    public static final String CAPTION = "Captured proposition";
    private SelfContainedAuditFixture() { }
    public static String id(int index) { return "case-" + index; }
    public static String payload(int index) { return "captured " + "state <value> & \"quoted\" ♠ ".repeat(20) + "FINAL-JSON-" + index; }
    public static String narrative(int index) {
        return (index == 0 ? "\r\n" : "\n") + "\t" + "recorded <state> & &#13; \"value\"\n".repeat(25) + "FINAL-TEXT-" + index + "\n\t ";
    }
    public static String png(int index) throws Exception {
        var image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) image.setRGB(x, y, index == 0 ? 0xff3366aa : 0xffaa6633);
        try (var bytes = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", bytes); return Base64.getEncoder().encodeToString(bytes.toByteArray());
        }
    }
    public static Path publish(boolean extent, boolean ui, boolean combined, String run) throws Exception {
        Path report = TestReportPaths.report("self-contained-" + run + "-" + extent + "-" + ui + "-" + combined + ".html");
        var settings = new ReportingSettings(240, 12000);
        try (AtfReportPorts.Session session = extent ? new ExtentReportSession(report, settings) : new HtmlReportSession(report, settings)) {
            for (int index = 0; index < (combined ? 2 : 1); index++) {
                var status = index == 0 ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
                var raw = session.startScenario("InlineAudit" + index, "atf.fixture", CAPTION, List.of("component", "synthetic", ui ? "ui" : "model"));
                var timing = new ReportPhaseTiming(Duration.ofMillis(2345 + index), 1);
                var comparisons = List.of(new AtfAssertion("Recorded count", "41", index == 0 ? "41" : "42", index == 0),
                        new AtfAssertion("Stable input identity", id(index), id(index), true));
                if (ui) {
                    try (var scenario = UiExtentReportSession.adaptScenario(raw,
                            ReportEvidenceProfile.forScreenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT), CAPTION);
                         var phase = scenario.startPhase("TEST BODY")) {
                        phase.executionTiming(timing); phase.observation(observation(index));
                        phase.uiValidation("Captured checkpoint", "synthetic surface / no device", png(index), comparisons, status);
                        phase.complete(status); scenario.complete(status);
                    }
                } else try (raw; var phase = raw.startPhase("TEST BODY")) {
                    phase.executionTiming(timing); phase.observation(observation(index));
                    try (var step = phase.startAssertionStep("Measured comparisons")) { step.assertions(comparisons); step.complete(status); }
                    phase.complete(status); raw.complete(status);
                }
            }
        }
        return report;
    }
    private static ReportObservation observation(int index) throws Exception {
        String json = new ObjectMapper().writeValueAsString(Map.of("id", id(index), "large", 9007199254740993L,
                "before", "READY", "after", index == 0 ? "COMPLETE" : "REJECTED", "payload", payload(index), "token", CANARY));
        return new ReportObservation("Captured facts", List.of(new ReportField("Evidence identity", id(index)),
                new ReportField("Provenance", "Synthetic renderer contract; no Android or business execution")),
                List.of(new ReportObservation.Body("Complete captured state", json, ReportObservation.Format.JSON),
                        new ReportObservation.Body("Complete recorded context", narrative(index), ReportObservation.Format.TEXT)), List.of());
    }
}
