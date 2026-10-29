package atf.reporting.extent;
import atf.reporting.contract.evidence.ReportEvidenceOwner;
import atf.reporting.console.AtfConsoleEvent;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.contract.ReportStatusAggregator;
import atf.reporting.extent.session.ExtentReportTree;
import atf.reporting.model.ReportScreenshot;
import atf.reporting.semantic.AtfReportSemanticRecorder;

import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import com.aventstack.extentreports.ExtentTest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

final class ExtentValidationScope implements AtfReportPorts.Validation {
    private final ExtentReportTree tree;
    private final ExtentRequestScope owner;
    private final ExtentTest validation;
    private final String validationTitle;
    private final AtfReportSemanticRecorder.ValidationHandle semantic;
    private final ReportStatusAggregator childStatuses = new ReportStatusAggregator();
    private List<AtfAssertion> assertions = List.of();
    private final List<AtfUiActionRecord> uiActions = new ArrayList<>();
    private ReportUiActionDetailsMode detailsMode;
    private final List<ReportScreenshot> screenshots = new ArrayList<>();
    private final List<ReportScreenshot> mediaScreenshots = new ArrayList<>();
    private ReportEvidenceOwner evidenceOwner = ReportEvidenceOwner.UNSPECIFIED;

    ExtentValidationScope(ExtentReportTree tree, ExtentRequestScope owner, ExtentTest validation,
                          String validationTitle, AtfReportSemanticRecorder.ValidationHandle semantic) {
        this.tree = tree;
        this.owner = owner;
        this.validation = validation;
        this.validationTitle = validationTitle;
        this.semantic = semantic;
        this.detailsMode = tree.defaultDetailsMode();
        tree.markStarted(validation);
    }

    @Override public void evidenceOwner(ReportEvidenceOwner value) {
        ensureOpen();
        evidenceOwner = Objects.requireNonNull(value, "evidenceOwner");
        semantic.evidenceOwner(value);
    }

    @Override public void assertions(Collection<AtfAssertion> rows) {
        ensureOpen();
        assertions = rows == null ? List.of() : List.copyOf(rows);
        semantic.assertions(assertions);
    }

    @Override public void uiActions(Collection<AtfUiActionRecord> actions) {
        ensureOpen();
        var stable = actions == null ? List.<AtfUiActionRecord>of() : List.copyOf(actions);
        semantic.uiActions(stable);
        uiActions.addAll(stable);
    }

    @Override public void uiActionDetailsMode(ReportUiActionDetailsMode mode) {
        ensureOpen();
        detailsMode = Objects.requireNonNull(mode, "mode");
    }

    @Override public void screenshot(String title, String base64Png) {
        ensureOpen();
        if (base64Png == null || base64Png.isBlank()) {
            throw new IllegalArgumentException("base64Png must not be blank");
        }
        screenshots.add(new ReportScreenshot(title, base64Png));
        semantic.screenshot();
    }

    @Override public void mediaScreenshot(String title, String base64Png) {
        ensureOpen();
        if (base64Png == null || base64Png.isBlank()) {
            throw new IllegalArgumentException("base64Png must not be blank");
        }
        mediaScreenshots.add(new ReportScreenshot(title, base64Png));
        semantic.screenshot();
    }

    @Override public void complete(AtfReportStatus status) {
        complete(status, false);
    }

    private void complete(AtfReportStatus status, boolean failure) {
        tree.ensureOpen();
        semantic.ensureOpen();
        AtfReportStatus effective = childStatuses.resolve(status, assertions);
        if (!screenshots.isEmpty() && !mediaScreenshots.isEmpty()) throw new IllegalStateException("A validation cannot mix inline and media screenshots");
        semantic.complete(effective);
        boolean screenshotOptional = failure && screenshots.isEmpty() && mediaScreenshots.isEmpty();
        if (screenshotOptional) {
            tree.renderUiActionsAsMedia(validation, uiActions, detailsMode);
        } else if (mediaScreenshots.isEmpty() && screenshots.size() <= 1) {
            tree.renderUiActions(validation, uiActions, screenshots, detailsMode);
            if (uiActions.stream().noneMatch(action -> action.source() == AtfUiEvidenceSource.ACTION)) {
                tree.renderScreenshots(validation, screenshots);
            }
        } else {
            // A unified flow can aggregate many checkpoint screenshots into one
            // validation. Keep the ordered action table singular and publish all
            // screenshots as separate evidence links after it.
            tree.renderUiActions(validation, uiActions, List.of(), detailsMode);
            tree.renderScreenshots(validation,
                    screenshots.isEmpty() ? mediaScreenshots : screenshots);
        }
        tree.renderAssertions(validation, assertions, evidenceOwner);
        tree.complete(validation, effective);
        tree.markEnded(validation);
        tree.consoleCompleted(AtfConsoleEvent.Scope.VALIDATION,
                validationTitle, effective, validation);
        owner.record(effective);
    }

    @Override public AtfReportPorts.Request startRequest(String title, String method, String path) {
        ensureOpen();
        String requestTitle = tree.renderRequestTitle(title, method, path);
        tree.consoleStarted(AtfConsoleEvent.Scope.REQUEST, requestTitle);
        return new ExtentRequestScope(tree, owner.phase(), validation.createNode(requestTitle), requestTitle,
                semantic.startRequest(title, method, path), this::recordChildStatus);
    }

    private void recordChildStatus(AtfReportStatus status) {
        childStatuses.record(status);
    }

    @Override public void completeFailure(AtfReportStatus status) {
        // A failure may happen before a page can be captured, but an already
        // supplied screenshot is still first-class evidence and must not be
        // discarded by the failure completion path.
        complete(status, true);
    }

    @Override public void close() { if (!semantic.isComplete()) completeFailure(AtfReportStatus.FAIL); }

    private void ensureOpen() {
        tree.ensureOpen();
        semantic.ensureOpen();
    }
}
