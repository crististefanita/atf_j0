package atf.reporting.ui.failure;

import atf.reporting.contract.*;
import atf.reporting.contract.evidence.ReportEvidenceOwner;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.reporting.markup.AtfReportEvidenceKeys;
import atf.reporting.ui.model.*;
import java.util.*;

/** Renderer-neutral failure publication; no browser, JUnit, retry or business verdict evaluation. */
public final class UiFailureEvidencePublisher {
    private final AtfReportPorts.Phase phase;
    private final ReportScreenshotPolicy screenshotPolicy;
    private final ReportUiActionDetailsMode detailsMode;

    public UiFailureEvidencePublisher(AtfReportPorts.Phase phase, ReportScreenshotPolicy screenshotPolicy,
                                      ReportUiActionDetailsMode detailsMode) {
        this.phase = Objects.requireNonNull(phase, "phase");
        this.screenshotPolicy = Objects.requireNonNull(screenshotPolicy, "screenshotPolicy");
        this.detailsMode = Objects.requireNonNull(detailsMode, "detailsMode");
    }

    public void publish(String title, String target, Collection<AtfUiActionRecord> actions,
                         Throwable failure, String base64Png, boolean reportedCheckpoint) {
        String safeTitle = title == null || title.isBlank() ? "UI audit failure" : title.trim();
        var context = new UiFailureContext(failure, actions);
        var screenshot = UiFailureScreenshotState.resolve(screenshotPolicy, base64Png);
        String screenshotDescription = screenshot.description(screenshotPolicy);
        try (var request = phase.startRequest(AtfGherkinKeyword.THEN.text() + " " + safeTitle, "", "")) {
            request.detail(AtfReportEvidenceKeys.REQUEST_ACTION, safeTitle);
            if (target != null && !target.isBlank()) request.detail(AtfReportEvidenceKeys.REQUEST_TARGET, target);
            request.detail(AtfReportEvidenceKeys.EVIDENCE_TYPE, AtfReportEvidenceKeys.UI_FAILURE);
            request.detail(AtfReportEvidenceKeys.EVIDENCE_POLICY, screenshotDescription);
            var validation = request.startValidation("Failure evidence");
            validation.evidenceOwner(ReportEvidenceOwner.UI);
            validation.uiActionDetailsMode(detailsMode);
            var projected = context.projectedActions();
            if (!projected.isEmpty()) validation.uiActions(projected);
            int sequence = Math.addExact(projected.stream().mapToInt(AtfUiActionRecord::sequence).max().orElse(0), 1);
            var observations = new ArrayList<AtfUiActionRecord>();
            for (var diagnostic : context.diagnostics()) {
                observations.add(observation(sequence++, "Failure diagnostic", diagnostic.relation(),
                        "original exception or recorded observation; no inferred dispatch or network state", diagnostic.text()));
            }
            observations.add(observation(sequence++, "Screenshot", "current page",
                    "capture follows configured failure policy", screenshotDescription));
            if (reportedCheckpoint) observations.add(observation(sequence, "Propagated checkpoint failure",
                    "reported UI assertion", "original failure remains in Fails", context.summary()));
            validation.uiActions(observations);
            if (screenshot == UiFailureScreenshotState.CAPTURED) {
                if (target == null || target.isBlank()) throw new IllegalArgumentException(
                        "A screenshot target is required when screenshot evidence is published");
                validation.screenshot("UI failure screenshot", base64Png);
            }
            if (!reportedCheckpoint) validation.assertions(List.of(new AtfAssertion(
                    "Failure cause", "no failure", context.summary(), false)));
            validation.completeFailure(AtfReportStatus.FAIL);
            request.complete(AtfReportStatus.FAIL);
        }
    }

    private static AtfUiActionRecord observation(int sequence, String operation, String target, String expected, String observed) {
        return new AtfUiActionRecord(sequence, operation, target, expected, observed, 0, AtfReportStatus.INFO, AtfUiEvidenceSource.OBSERVE);
    }
}
