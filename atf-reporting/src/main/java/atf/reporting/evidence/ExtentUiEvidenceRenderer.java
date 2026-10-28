package atf.reporting.evidence;
import atf.reporting.contract.evidence.ReportEvidenceOwner;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.evidence.ui.ExtentUiActionRenderer;
import atf.reporting.evidence.ui.ExtentUiAssertionRenderer;
import atf.reporting.evidence.ui.ExtentUiObservationRenderer;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.reporting.model.ReportScreenshot;
import atf.reporting.security.SensitiveDataRedactor;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import com.aventstack.extentreports.ExtentTest;

import java.util.Collection;
import java.util.List;

/** Coordinates specialized UI evidence renderers without owning their markup. */
public final class ExtentUiEvidenceRenderer {
    private final ExtentUiActionRenderer actionRenderer;
    private final ExtentUiObservationRenderer observationRenderer;
    private final ExtentUiAssertionRenderer assertionRenderer;

    ExtentUiEvidenceRenderer(ReportingSettings settings, SensitiveDataRedactor redactor) {
        this(settings, redactor, ReportUiActionDetailsMode.INCLUDED_COLLAPSED);
    }

    ExtentUiEvidenceRenderer(ReportingSettings settings, SensitiveDataRedactor redactor,
                             ReportUiActionDetailsMode detailsMode) {
        actionRenderer = new ExtentUiActionRenderer(settings, redactor, detailsMode);
        observationRenderer = new ExtentUiObservationRenderer(settings, redactor);
        assertionRenderer = new ExtentUiAssertionRenderer(settings, redactor);
    }

    void renderUiActions(ExtentTest node, List<AtfUiActionRecord> actions) {
        renderUiActions(node, actions, List.of());
    }

    void renderUiActions(ExtentTest node, List<AtfUiActionRecord> actions,
                         List<ReportScreenshot> screenshots) {
        if (actions == null || actions.isEmpty()) return;
        actionRenderer.render(node, actions.stream()
                .filter(action -> action.source() == AtfUiEvidenceSource.ACTION).toList(), screenshots);
        observationRenderer.render(node, actions.stream()
                .filter(action -> action.source() == AtfUiEvidenceSource.OBSERVE).toList());
    }

    /** Renders the action table only; the caller adds native media separately. */
    void renderUiActionsAsMedia(ExtentTest node, List<AtfUiActionRecord> actions) {
        if (actions == null || actions.isEmpty()) return;
        actionRenderer.renderAsMedia(node, actions.stream()
                .filter(action -> action.source() == AtfUiEvidenceSource.ACTION).toList());
        observationRenderer.render(node, actions.stream()
                .filter(action -> action.source() == AtfUiEvidenceSource.OBSERVE).toList());
    }

    void renderAssertions(ExtentTest node, Collection<AtfAssertion> assertions) {
        renderAssertions(node, assertions, ReportEvidenceOwner.UNSPECIFIED);
    }

    void renderAssertions(ExtentTest node, Collection<AtfAssertion> assertions, ReportEvidenceOwner owner) {
        assertionRenderer.render(node, assertions, owner);
    }
}
