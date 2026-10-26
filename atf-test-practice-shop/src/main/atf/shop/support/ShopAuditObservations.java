package atf.shop.support;

import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.ui.playwright.PlaywrightActionSequence;
import atf.ui.playwright.UiScreenValidation;
import atf.ui.audit.UiScreenAudit;
import com.microsoft.playwright.Page;

import java.util.Collection;
import java.util.Objects;

/**
 * Publishes dynamic, renderer-neutral audit observations for one purchase
 * checkpoint. Assertions keep the verdict; observations explain the evidence
 * boundary that produced it.
 */
public final class ShopAuditObservations {
    private ShopAuditObservations() {
    }

    public static void recordCheckpoint(PlaywrightActionSequence actions,
                                        Page page,
                                        ShopPurchaseCheckpoint checkpoint,
                                        UiScreenValidation.Evidence screenshot) {
        Objects.requireNonNull(actions, "actions");
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(checkpoint, "checkpoint");
        Objects.requireNonNull(screenshot, "screenshot");

        recordReplayCheckpoint(actions, page, checkpoint, screenshot);
        recordScreenContract(actions, page, checkpoint.title(), checkpoint.assertions(), screenshot,
                checkpoint.passed());

        int exchange = 0;
        for (ReportExecutionEvidence evidence : checkpoint.apiEvidence()) {
            if (evidence == null) {
                continue;
            }
            var request = evidence.request();
            var response = evidence.response();
            if (request == null || response == null) continue;
            exchange++;
            String route = request.method() + " " + request.path();
            actions.observation(
                    "Audit API exchange #" + exchange,
                    route,
                    "HTTP response and execution evidence are present",
                    "status=" + response.status()
                            + "; auth=" + safe(request.authProfile())
                            + "; transport=" + evidence.provenance().reportLabel()
                            + "; attempts=" + evidence.attempts());
        }
    }

    /**
     * Publishes the smallest reader-facing replay contract for one checkpoint.
     *
     * <p>This is deliberately an observation, not another browser action: the
     * action table answers "what did the test do?", while this row tells a
     * reader without ATF source what to verify before continuing.</p>
     */
    private static void recordReplayCheckpoint(PlaywrightActionSequence actions,
                                               Page page,
                                               ShopPurchaseCheckpoint checkpoint,
                                               UiScreenValidation.Evidence screenshot) {
        int total = checkpoint.assertions().size();
        long passed = checkpoint.assertions().stream().filter(AtfAssertion::passed).count();
        boolean terminal = checkpoint.keyword() == AtfGherkinKeyword.THEN;
        actions.observation(
                "Replay checkpoint",
                safeUrl(page),
                "perform the actions above; verify '" + checkpoint.title()
                        + "' and continue only when visible assertions pass",
                "status=" + (checkpoint.passed() ? "PASS" : "FAIL")
                        + "; visible assertions=" + passed + "/" + total
                        + "; terminal=" + (terminal ? "success condition" : "continue")
                        + "; screenshot=" + (screenshot.hasScreenshotPayload() ? "valid PNG" : "not captured")
                        + "; checkpoint=" + checkpoint.title());
    }

    /** Adds the same screen audit to the account/catalog compatibility path. */
    public static void recordScreenContract(PlaywrightActionSequence actions,
                                            Page page,
                                            String title,
                                            Collection<AtfAssertion> assertions,
                                            UiScreenValidation.Evidence screenshot,
                                            boolean passed) {
        Objects.requireNonNull(actions, "actions");
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(screenshot, "screenshot");

        UiScreenAudit.record(actions, title, safeUrl(page),
                assertions == null ? 0 : assertions.size(), screenshot, passed);
    }

    private static String safeUrl(Page page) {
        try {
            return safe(page.url());
        } catch (RuntimeException ignored) {
            return "unavailable";
        }
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "not declared" : value.trim();
    }
}
