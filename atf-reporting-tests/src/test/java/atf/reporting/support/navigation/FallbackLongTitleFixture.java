package atf.reporting.support.navigation;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.html.HtmlReportSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Original long scenario/class names and a sibling with independent assertion evidence. */
public final class FallbackLongTitleFixture {
    public static final String TITLE = "Long purchase context ".repeat(15) + "UNBROKEN".repeat(35);
    public static final String CLASS_NAME = "VeryLongScenarioClass".repeat(20);
    public record Published(Path path, String firstId, String siblingId) { }
    private FallbackLongTitleFixture() { }

    public static Published publish() throws Exception {
        Path path = Files.createTempDirectory("atf-fallback-title-").resolve("long-title.html");
        String firstId;
        String siblingId;
        try (var session = new HtmlReportSession(path)) {
            firstId = scenario(session, CLASS_NAME, TITLE, "original evidence");
            siblingId = scenario(session, "SiblingClass", "short sibling", "sibling evidence");
        }
        System.out.println("[ATF] Fallback long-title report: " + path.toAbsolutePath());
        return new Published(path, firstId, siblingId);
    }

    private static String scenario(HtmlReportSession session, String className, String title, String marker) {
        var scenario = session.startScenario(className, title, List.of("component"));
        var phase = scenario.startPhase("TEST BODY");
        var step = phase.startAssertionStep("Independent context");
        step.assertions(List.of(new AtfAssertion(marker, "retained", "retained", true)));
        step.complete(AtfReportStatus.PASS);
        phase.complete(AtfReportStatus.PASS);
        scenario.complete(AtfReportStatus.PASS);
        return scenario.id();
    }
}
