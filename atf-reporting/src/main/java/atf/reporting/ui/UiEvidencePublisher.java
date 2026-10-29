package atf.reporting.ui;
import atf.reporting.contract.ReportStatusAggregator;
import atf.reporting.contract.evidence.ReportEvidenceOwner;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.api.evidence.ApiExchangeEvidence;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;

import atf.reporting.model.ReportScreenshot;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.failure.UiFailureEvidencePublisher;
import atf.reporting.ui.validation.UiApiValidationEvidence;
import atf.reporting.markup.AtfReportEvidenceKeys;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Publishes the concrete UI evidence attached to one report phase.
 *
 * <p>{@link UiExtentReportSession} owns lifecycle state; this component owns
 * the evidence contract. Keeping those responsibilities separate prevents
 * screenshot, action and assertion rules from being duplicated across the
 * normal, media and failure paths.</p>
 */
public final class UiEvidencePublisher {
    private static final String UI_VALIDATION = "UI validation";
    private static final String API_VALIDATION = "API validation";

    private final AtfReportPorts.Phase phase;
    private final ReportScreenshotPolicy screenshotPolicy;
    private final ReportUiActionDetailsMode detailsMode;
    private final ApiEvidencePublisher apiEvidence = new ApiEvidencePublisher();

    UiEvidencePublisher(AtfReportPorts.Phase phase) {
        this(phase, ReportScreenshotPolicy.EACH_CHECKPOINT);
    }

    UiEvidencePublisher(AtfReportPorts.Phase phase,
                        ReportScreenshotPolicy screenshotPolicy) {
        this(phase, screenshotPolicy, ReportUiActionDetailsMode.INCLUDED_COLLAPSED);
    }

    UiEvidencePublisher(AtfReportPorts.Phase phase, ReportScreenshotPolicy screenshotPolicy,
                        ReportUiActionDetailsMode detailsMode) {
        this.phase = Objects.requireNonNull(phase, "phase");
        this.screenshotPolicy = Objects.requireNonNull(screenshotPolicy, "screenshotPolicy");
        this.detailsMode = Objects.requireNonNull(detailsMode, "detailsMode");
    }

    void publishOutcome(String keyword, String title, String target,
                       AtfReportStatus status) {
        String stepTitle = stepTitle(keyword, title);
        var request = phase.startRequest(stepTitle, "", "");
        publishScope(() -> {
            request.detail(AtfReportEvidenceKeys.REQUEST_ACTION, stepTitle);
            detailTarget(request, target);
            request.detail(AtfReportEvidenceKeys.OUTCOME_TYPE, AtfReportEvidenceKeys.ARTIFACT_PUBLICATION);
            request.complete(defaultStatus(status, AtfReportStatus.PASS));
        }, request::close);
    }

    /**
     * Publishes precondition checks under SETUP. The compact setup table
     * summarizes readiness; captured UI actions and selected API exchanges
     * remain separate evidence under the same checkpoint. Setup evidence is
     * not UI validation; screen assertions belong to TEST BODY.
     */
    void publishSetup(String keyword, String title, String target,
                      Collection<AtfAssertion> assertions, AtfReportStatus status) {
        publishSetup(keyword, title, target, assertions, List.of(), UiApiValidationEvidence.empty(), status);
    }

    void publishSetup(String keyword, String title, String target,
                      Collection<AtfAssertion> assertions,
                      Collection<AtfUiActionRecord> actions,
                      UiApiValidationEvidence apiValidation,
                      AtfReportStatus status) {
        publishSetup(keyword, title, target, assertions, actions, apiValidation, null, status);
    }

    void publishSetup(String keyword, String title, String target,
                      Collection<AtfAssertion> assertions,
                      Collection<AtfUiActionRecord> actions,
                      UiApiValidationEvidence apiValidation,
                      ReportScreenshot screenshot,
                      AtfReportStatus status) {
        var initialStatus = new ReportStatusAggregator().resolve(defaultStatus(status, AtfReportStatus.PASS), assertions);
        String stepTitle = stepTitle(keyword, title);
        var request = phase.startRequest(stepTitle, "", "");
        publishScope(() -> {
            var effectiveStatus = initialStatus;
            request.detail(AtfReportEvidenceKeys.REQUEST_ACTION, stepTitle);
            detailTarget(request, target);
            request.detail(AtfReportEvidenceKeys.EVIDENCE_TYPE, AtfReportEvidenceKeys.REPORT_SETUP);
            request.detail(AtfReportEvidenceKeys.SETUP_RESULT,
                    effectiveStatus.displayValue());
            if (assertions != null) {
                int index = 0;
                for (AtfAssertion assertion : assertions) {
                    if (assertion == null) continue;
                    index++;
                    request.detail(AtfReportEvidenceKeys.setupCheck(index),
                            assertion.name() + " | expected=" + assertion.expected()
                                    + " | actual=" + assertion.actual()
                                    + " | result=" + assertion.resultStatus().displayValue());
                }
            }
            request.uiActionDetailsMode(detailsMode);
            if (actions != null && !actions.isEmpty()) request.uiActions(actions);
            if (screenshot != null) request.screenshot(screenshot.title(), screenshot.base64Png());
            if (!apiValidation.isEmpty()) {
                effectiveStatus = AtfReportStatus.worst(effectiveStatus, apiValidation.status());
                var validation = request.startValidation(API_VALIDATION);
                publishScope(() -> {
                    apiEvidence.publish(validation, apiValidation);
                    validation.complete(apiValidation.status());
                }, validation::close);
            }
            request.complete(effectiveStatus);
        }, request::close);
    }

    void publishAction(String keyword, String title, String target, String base64Png,
                       Collection<AtfAssertion> assertions, AtfReportStatus status,
                       UiValidationKind validationKind, Collection<AtfUiActionRecord> actions) {
        publishAction(keyword, title, target, base64Png, assertions, status,
                validationKind, actions, UiApiValidationEvidence.empty(), ReportScreenshotPolicy.CapturePoint.CHECKPOINT);
    }

    /** Publishes separately owned UI and API verdicts for a single journey checkpoint. */
    void publishAction(String keyword, String title, String target, String base64Png,
                       Collection<AtfAssertion> uiAssertions, AtfReportStatus uiStatus,
                       UiValidationKind validationKind, Collection<AtfUiActionRecord> actions,
                       UiApiValidationEvidence apiValidation,
                       ReportScreenshotPolicy.CapturePoint capturePoint) {
        String stepTitle = stepTitle(keyword, title);
        var request = phase.startRequest(stepTitle, "", "");
        publishScope(() -> {
            request.detail(AtfReportEvidenceKeys.REQUEST_ACTION, stepTitle);
            detailTarget(request, target);
            boolean visualValidation = validationKind.hasVisualEvidence();
            boolean hasApiValidation = !java.util.Objects.requireNonNull(apiValidation, "apiValidation").isEmpty();
            if (visualValidation) {
                request.detail(AtfReportEvidenceKeys.EVIDENCE_TYPE, AtfReportEvidenceKeys.UI_VALIDATION);
                request.detail(AtfReportEvidenceKeys.EVIDENCE_POLICY,
                        "one Base64 screenshot link per visual state; policy="
                                + screenshotPolicy.configValue());
            }
            if (visualValidation || (uiAssertions != null && !uiAssertions.isEmpty())) {
                var validation = request.startValidation(validationKind.reportTitle());
                publishScope(() -> {
                    publishActions(validation, actions);
                    publishScreenshot(validation, target, title, base64Png, uiStatus, "UI assertion", capturePoint);
                    validation.assertions(uiAssertions);
                    validation.complete(defaultStatus(uiStatus, AtfReportStatus.PASS));
                }, validation::close);
            }
            if (hasApiValidation) {
                var validation = request.startValidation(API_VALIDATION);
                publishScope(() -> {
                    apiEvidence.publish(validation, apiValidation);
                    validation.complete(apiValidation.status());
                }, validation::close);
            }
            request.complete(AtfReportStatus.worst(defaultStatus(uiStatus, AtfReportStatus.PASS),
                    hasApiValidation ? apiValidation.status() : AtfReportStatus.INFO));
        }, request::close);
    }

    /** Publishes the buffered phase as one Gherkin proposition with grouped evidence. */
    void publishUnifiedFlow(String scenarioTitle, String phaseName,
                            List<UiUnifiedFlowCheckpoint> checkpoints,
                            AtfReportStatus phaseStatus) {
        if (checkpoints == null || checkpoints.isEmpty()) return;
        String flowTitle = unifiedProposition(checkpoints);
        AtfReportStatus flowStatus = AtfReportStatus.orInfo(phaseStatus);
        AtfReportStatus uiStatus = AtfReportStatus.INFO;
        AtfReportStatus apiStatus = AtfReportStatus.INFO;
        List<AtfAssertion> assertions = checkpoints.stream()
                .flatMap(checkpoint -> checkpoint.assertions().stream()).toList();
        List<AtfUiActionRecord> actions = checkpoints.stream()
                .flatMap(checkpoint -> checkpoint.actions().stream()).toList();
        List<UiApiValidationEvidence.Exchange> apiExchanges = checkpoints.stream()
                .flatMap(checkpoint -> checkpoint.apiValidation().exchanges().stream()).toList();
        List<AtfAssertion> apiAssertions = checkpoints.stream()
                .flatMap(checkpoint -> checkpoint.apiValidation().assertions().stream()).toList();
        boolean hasUiEvidence = false;
        for (UiUnifiedFlowCheckpoint checkpoint : checkpoints) {
            AtfReportStatus checkpointStatus = effectiveStatus(checkpoint);
            flowStatus = AtfReportStatus.worst(flowStatus, checkpointStatus);
            if (isUiEvidence(checkpoint)) {
                hasUiEvidence = true;
                uiStatus = AtfReportStatus.worst(uiStatus, checkpoint.status());
            }
            if (!checkpoint.apiValidation().isEmpty()) {
                apiStatus = AtfReportStatus.worst(apiStatus, checkpoint.apiValidation().status());
            }
        }

        var finalFlowStatus = flowStatus;
        var finalUiStatus = uiStatus;
        var finalApiStatus = apiStatus;
        boolean publishUi = hasUiEvidence;
        var flow = phase.startRequest(flowTitle, "", "");
        publishScope(() -> {
            flow.detail(AtfReportEvidenceKeys.EVIDENCE_TYPE,
                    AtfReportEvidenceKeys.UNIFIED_UI_FLOW);
            flow.detail(AtfReportEvidenceKeys.FLOW_MODE,
                    ReportEvidenceFlowMode.UNIFIED_FLOW.configValue());
            if (hasText(scenarioTitle)) {
                flow.detail(AtfReportEvidenceKeys.FLOW_SCENARIO, scenarioTitle.trim());
            }
            if (hasText(phaseName)) {
                flow.detail(AtfReportEvidenceKeys.FLOW_PHASE, phaseName);
            }

            if (publishUi) {
                var validation = flow.startValidation(UI_VALIDATION);
                publishScope(() -> {
                    publishActions(validation, actions);
                    for (UiUnifiedFlowCheckpoint checkpoint : checkpoints) {
                        publishUnifiedScreenshot(validation, checkpoint,
                                effectiveStatus(checkpoint));
                    }
                    validation.assertions(assertions);
                    validation.complete(finalUiStatus);
                }, validation::close);
            }
            if (!apiExchanges.isEmpty() || !apiAssertions.isEmpty()) {
                var apiValidation = flow.startValidation(API_VALIDATION);
                publishScope(() -> {
                    apiEvidence.publish(apiValidation,
                            new UiApiValidationEvidence(apiAssertions, apiExchanges));
                    apiValidation.complete(finalApiStatus);
                }, apiValidation::close);
            }
            flow.complete(finalFlowStatus);
        }, flow::close);
    }

    private static boolean isUiEvidence(UiUnifiedFlowCheckpoint checkpoint) {
        return checkpoint.validationKind().hasVisualEvidence()
                || !checkpoint.assertions().isEmpty() || !checkpoint.actions().isEmpty()
                || hasText(checkpoint.base64Png());
    }

    private static AtfReportStatus effectiveStatus(UiUnifiedFlowCheckpoint checkpoint) {
        return AtfReportStatus.worst(checkpoint.status(), checkpoint.apiValidation().status());
    }

    private void publishUnifiedScreenshot(AtfReportPorts.Validation validation,
                                          UiUnifiedFlowCheckpoint checkpoint,
                                          AtfReportStatus status) {
        if (!shouldPublish(checkpoint.base64Png(), checkpoint.capturePoint())) return;
        requireTargetForScreenshot(checkpoint.target());
        String title = "UI screenshot — " + checkpoint.title() + " — "
                + defaultStatus(status, AtfReportStatus.PASS);
        if (checkpoint.mediaScreenshot()) {
            validation.mediaScreenshot(title, checkpoint.base64Png());
        } else {
            validation.screenshot("UI assertion — " + checkpoint.title() + " — "
                    + defaultStatus(status, AtfReportStatus.PASS), checkpoint.base64Png());
        }
    }

    private static String unifiedProposition(List<UiUnifiedFlowCheckpoint> checkpoints) {
        UiUnifiedFlowCheckpoint proposition = checkpoints.get(checkpoints.size() - 1);
        for (UiUnifiedFlowCheckpoint checkpoint : checkpoints) {
            if (AtfGherkinKeyword.THEN.text().equalsIgnoreCase(checkpoint.keyword())) {
                proposition = checkpoint;
            }
        }
        String title = proposition.gherkinTitle();
        return hasText(title) ? title : AtfGherkinKeyword.THEN.text() + " UI flow completed";
    }

    /** Keeps every phase's verdict and causal API evidence inside one scenario proposition. */
    void publishCrossPhaseUnifiedFlow(String scenarioTitle,
                                      List<UiScenarioLifecycle.PhaseCheckpoint> entries,
                                      AtfReportStatus finalStatus) {
        if (entries.isEmpty()) return;
        Optional<CompactUiFlow> compact = compactUiOnlyFlow(entries, finalStatus);
        List<UiUnifiedFlowCheckpoint> checkpoints = entries.stream()
                .map(UiScenarioLifecycle.PhaseCheckpoint::checkpoint).toList();
        var flow = phase.startRequest(unifiedProposition(checkpoints), "", "");
        publishScope(() -> {
            flow.detail(AtfReportEvidenceKeys.EVIDENCE_TYPE,
                    AtfReportEvidenceKeys.UNIFIED_UI_FLOW);
            flow.detail(AtfReportEvidenceKeys.FLOW_MODE,
                    ReportEvidenceFlowMode.UNIFIED_FLOW.configValue());
            if (compact.isPresent()) {
                flow.detail(AtfReportEvidenceKeys.FLOW_PRESENTATION,
                        AtfReportEvidenceKeys.COMPACT_UI_ONLY);
            }
            if (hasText(scenarioTitle)) flow.detail(AtfReportEvidenceKeys.FLOW_SCENARIO, scenarioTitle);
            flow.detail(AtfReportEvidenceKeys.FLOW_PHASE,
                    entries.stream().map(UiScenarioLifecycle.PhaseCheckpoint::phase)
                            .distinct().collect(java.util.stream.Collectors.joining(" → ")));
            AtfReportStatus aggregate = AtfReportStatus.orInfo(finalStatus);
            int index = 0;
            for (var entry : entries) {
                UiUnifiedFlowCheckpoint checkpoint = entry.checkpoint();
                AtfReportStatus checkpointStatus = effectiveStatus(checkpoint);
                aggregate = AtfReportStatus.worst(aggregate,
                        AtfReportStatus.worst(AtfReportStatus.orInfo(entry.phaseStatus()), checkpointStatus));
                String prefix = "flow.checkpoint." + (++index);
                flow.detail(prefix + ".phase", entry.phase());
                flow.detail(prefix + ".phaseStatus", AtfReportStatus.orInfo(entry.phaseStatus()).displayValue());
                flow.detail(prefix + ".step", checkpoint.gherkinTitle());
                flow.detail(prefix + ".status", checkpointStatus.displayValue());
                if (compact.isEmpty() && (isUiEvidence(checkpoint)
                        || checkpoint.validationKind() == UiValidationKind.SETUP)) {
                    var validation = flow.startValidation(entry.phase() + " · "
                            + checkpoint.validationKind().reportTitle());
                    publishScope(() -> {
                        publishActions(validation, checkpoint.actions());
                        publishUnifiedScreenshot(validation, checkpoint, checkpointStatus);
                        validation.assertions(checkpoint.assertions());
                        validation.complete(checkpoint.status());
                    }, validation::close);
                }
                if (!checkpoint.apiValidation().isEmpty()) {
                    var validation = flow.startValidation(entry.phase() + " · API validation");
                    publishScope(() -> {
                        apiEvidence.publish(validation, checkpoint.apiValidation());
                        validation.complete(checkpoint.apiValidation().status());
                    }, validation::close);
                }
            }
            if (compact.isPresent()) {
                CompactUiFlow evidence = compact.orElseThrow();
                var validation = flow.startValidation("UI validation");
                publishScope(() -> {
                    publishActions(validation, evidence.actions());
                    if (evidence.terminalScreenshot() != null) {
                        publishUnifiedScreenshot(validation, evidence.terminalScreenshot(), evidence.status());
                    }
                    validation.assertions(evidence.assertions());
                    validation.complete(evidence.status());
                }, validation::close);
            }
            flow.complete(aggregate);
        }, flow::close);
    }

    /** Preserve source objects and encounter order; only their visual projection is joined. */
    private Optional<CompactUiFlow> compactUiOnlyFlow(
            List<UiScenarioLifecycle.PhaseCheckpoint> entries, AtfReportStatus finalStatus) {
        if (finalStatus != AtfReportStatus.PASS
                || entries.stream().map(UiScenarioLifecycle.PhaseCheckpoint::phase).distinct().count() < 2
                || entries.stream().noneMatch(entry -> "TEST BODY".equals(entry.phase()))
                || entries.stream().noneMatch(entry -> isUiEvidence(entry.checkpoint()))) {
            return Optional.empty();
        }
        var actions = new java.util.ArrayList<AtfUiActionRecord>();
        var assertions = new java.util.ArrayList<AtfAssertion>();
        UiUnifiedFlowCheckpoint terminalScreenshot = null;
        for (var entry : entries) {
            UiUnifiedFlowCheckpoint checkpoint = entry.checkpoint();
            if (entry.phaseStatus() != AtfReportStatus.PASS || checkpoint.status() != AtfReportStatus.PASS
                    || !checkpoint.apiValidation().isEmpty()
                    || checkpoint.actions().stream().anyMatch(action ->
                            action.status().severity() > AtfReportStatus.PASS.severity())
                    || checkpoint.assertions().stream().anyMatch(assertion ->
                            assertion.resultStatus() != AtfReportStatus.PASS)) return Optional.empty();
            actions.addAll(checkpoint.actions());
            assertions.addAll(checkpoint.assertions());
            if (shouldPublish(checkpoint.base64Png(), checkpoint.capturePoint())) {
                if (terminalScreenshot != null
                        || checkpoint.capturePoint() != ReportScreenshotPolicy.CapturePoint.FINAL
                        || !"TEST BODY".equals(entry.phase())
                        || checkpoint.mediaScreenshot()) return Optional.empty();
                terminalScreenshot = checkpoint;
            }
        }
        return Optional.of(new CompactUiFlow(List.copyOf(actions), List.copyOf(assertions),
                terminalScreenshot, AtfReportStatus.PASS));
    }

    private record CompactUiFlow(List<AtfUiActionRecord> actions, List<AtfAssertion> assertions,
                                 UiUnifiedFlowCheckpoint terminalScreenshot, AtfReportStatus status) { }

    /** Publishes an explicit standalone API trace with nested exchange evidence. */
    void publishApiTrace(String keyword, String title, String target,
                         UiApiValidationEvidence apiValidation,
                         AtfReportStatus status) {
        String sourceStep = stepTitle(keyword, title);
        AtfReportStatus effectiveStatus = AtfReportStatus.worst(
                defaultStatus(status, AtfReportStatus.PASS), apiValidation.status());
        // Keep the trace node as an exact Gherkin sibling. The evidence type,
        // parent-step detail and TRACE method already identify its role; a
        // second visible prefix would make the step noisy and non-semantic.
        var request = phase.startRequest(sourceStep, "TRACE", hasText(target) ? target : "network");
        publishScope(() -> {
            request.detail(AtfReportEvidenceKeys.EVIDENCE_TYPE, AtfReportEvidenceKeys.API_TRACE);
            request.detail("trace.parent.step", sourceStep);
            request.detail("trace.position", "causal sibling published immediately after UI step");
            var validation = request.startValidation(API_VALIDATION);
            publishScope(() -> {
                apiEvidence.publish(validation, apiValidation);
                validation.complete(apiValidation.status());
            }, validation::close);
            request.complete(effectiveStatus);
        }, request::close);
    }

    /**
     * Projects each API exchange into a request child of one semantic
     * {@code API validation}. This is deliberately the same typed request /
     * response / execution pipeline used by direct API reports: one exchange
     * owns one request node, one request JSON, one response JSON and its own
     * attempt history. The parent trace carries only causal context.
     */

    private static final class ApiEvidencePublisher {
        void publish(AtfReportPorts.Validation validation, UiApiValidationEvidence apiValidation) {
            validation.evidenceOwner(ReportEvidenceOwner.API);
            validation.assertions(apiValidation.assertions());
            int number = 0;
            for (var exchange : apiValidation.exchanges()) {
                var evidence = exchange.evidence();
                if (evidence.request() != null && evidence.response() != null) publishExchange(validation, exchange, ++number);
            }
        }

        private void publishExchange(AtfReportPorts.Validation validation, UiApiValidationEvidence.Exchange exchange, int number) {
            ReportExecutionEvidence evidence = exchange.evidence();
            var apiRequest = evidence.request();
            var request = validation.startRequest("Exchange " + number, apiRequest.method(), apiRequest.path());
            publishScope(() -> {
                request.detail("trace.exchange", "#" + number);
                ApiExchangeEvidence.publish(request, evidence);
                if (!exchange.assertions().isEmpty()) {
                    var checks = request.startValidation("API assertions");
                    publishScope(() -> {
                        checks.evidenceOwner(ReportEvidenceOwner.API);
                        checks.assertions(exchange.assertions());
                        checks.complete(exchange.status());
                    }, checks::close);
                }
                request.complete(exchange.status());
            }, request::close);
        }
    }

    void publishUiValidationAsMedia(String title, String target, String base64Png,
                                        Collection<AtfAssertion> assertions,
                                        Collection<AtfUiActionRecord> actions,
                                        AtfReportStatus status) {
        publishUiValidationAsMedia(title, target, base64Png, assertions, actions,
                ReportScreenshotPolicy.CapturePoint.CHECKPOINT, status);
    }

    void publishUiValidationAsMedia(String title, String target, String base64Png,
                                        Collection<AtfAssertion> assertions,
                                        Collection<AtfUiActionRecord> actions,
                                        ReportScreenshotPolicy.CapturePoint capturePoint,
                                        AtfReportStatus status) {
        String stepTitle = stepTitle(AtfGherkinKeyword.THEN.text(), title);
        var request = phase.startRequest(stepTitle, "", "");
        publishScope(() -> {
            request.detail("request.action", stepTitle);
            detailTarget(request, target);
            request.detail(AtfReportEvidenceKeys.EVIDENCE_TYPE, AtfReportEvidenceKeys.UI_VALIDATION);
            request.detail(AtfReportEvidenceKeys.EVIDENCE_POLICY,
                    "one visible Base64 image inside one safe clickable link; policy="
                    + screenshotPolicy.configValue());
            var validation = request.startValidation(UI_VALIDATION);
            publishScope(() -> {
                publishActions(validation, actions);
                if (shouldPublish(base64Png, capturePoint)) {
                    requireTargetForScreenshot(target);
                    validation.mediaScreenshot("UI screenshot — " + title + " — "
                            + defaultStatus(status, AtfReportStatus.PASS), base64Png);
                }
                validation.assertions(assertions);
                validation.complete(defaultStatus(status, AtfReportStatus.PASS));
            }, validation::close);
            request.complete(defaultStatus(status, AtfReportStatus.PASS));
        }, request::close);
    }

    void publishFailure(String title, String target, Collection<AtfUiActionRecord> actions,
                        Throwable failure, String base64Png) {
        publishFailure(title, target, actions, failure, base64Png, false);
    }

    void publishFailure(String title, String target, Collection<AtfUiActionRecord> actions,
                        Throwable failure, String base64Png, boolean reportedCheckpoint) {
        new UiFailureEvidencePublisher(phase, screenshotPolicy, detailsMode)
                .publish(title, target, actions, failure, base64Png, reportedCheckpoint);
    }

    private void publishActions(AtfReportPorts.Validation validation,
                                       Collection<AtfUiActionRecord> actions) {
        validation.evidenceOwner(ReportEvidenceOwner.UI);
        validation.uiActionDetailsMode(detailsMode);
        if (actions != null && !actions.isEmpty()) validation.uiActions(actions);
    }

    private void publishScreenshot(AtfReportPorts.Validation validation, String target,
                                   String title, String base64Png, AtfReportStatus status,
                                   String prefix, ReportScreenshotPolicy.CapturePoint capturePoint) {
        if (shouldPublish(base64Png, capturePoint)) {
            requireTargetForScreenshot(target);
            validation.screenshot(prefix + " — " + title + " — "
                    + defaultStatus(status, AtfReportStatus.PASS), base64Png);
        }
    }

    private boolean shouldPublish(String base64Png, ReportScreenshotPolicy.CapturePoint capturePoint) {
        return hasText(base64Png) && screenshotPolicy.captures(capturePoint);
    }

    boolean acceptsScreenshot(String base64Png,
                              ReportScreenshotPolicy.CapturePoint capturePoint) {
        return shouldPublish(base64Png, capturePoint);
    }

    private static void requireTargetForScreenshot(String target) {
        if (!hasText(target)) {
            throw new IllegalArgumentException(
                    "A screenshot target is required when screenshot evidence is published");
        }
    }

    private static void detailTarget(AtfReportPorts.Request request, String target) {
        if (hasText(target)) request.detail(AtfReportEvidenceKeys.REQUEST_TARGET, target);
    }

    private static String stepTitle(String keyword, String title) {
        return ((keyword == null ? "" : keyword.trim()) + " "
                + (title == null ? "" : title.trim())).trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static AtfReportStatus defaultStatus(AtfReportStatus status, AtfReportStatus fallback) {
        return status == null ? fallback : status;
    }

    /** One owned scope, one publication attempt; cleanup cannot replace or suppress itself onto the primary failure. */
    private static void publishScope(Runnable body, Runnable close) {
        Throwable primary = null;
        try {
            body.run();
        } catch (RuntimeException | Error failure) {
            primary = failure;
            throw failure;
        } finally {
            try {
                close.run();
            } catch (RuntimeException | Error cleanup) {
                if (primary == null) throw cleanup;
                if (cleanup != primary) primary.addSuppressed(cleanup);
            }
        }
    }
}
