package atf.ui.tests.extent.support.section.extract;
import atf.ui.tests.extent.support.audit.screen.ExistingReportStructureAuditor;
import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;

import atf.ui.playwright.AtfLocator;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.reporting.audit.ReportAuditSnapshot;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.Base64;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Browser adapter for legacy visual evidence; it emits facts only. */
public final class VisualEvidenceSectionExtractor {
    private final UiAuditFlow auditFlow;
    private final ExistingReportStructureAuditor structureAuditor;
    private final ReportUiLocators locators;

    public VisualEvidenceSectionExtractor(UiAuditFlow auditFlow,
                                          ExistingReportStructureAuditor structureAuditor,
                                          ReportUiLocators locators) {
        this.auditFlow = Objects.requireNonNull(auditFlow, "auditFlow");
        this.structureAuditor = Objects.requireNonNull(structureAuditor, "structureAuditor");
        this.locators = Objects.requireNonNull(locators, "locators");
    }

    public ReportAuditSnapshot.VisualEvidence extract(Page page) {
        auditFlow.actions().click(locators.firstTestOwnerAction(page), "class detail opens");
        Locator detail = locators.visibleTestDetail(page);
        Locator validations = locators.validationTables(detail);
        Locator disclosures = locators.evidenceDisclosures(detail);
        Locator sequences = locators.actionSequences(detail);
        int screenshotLinks = locators.base64ScreenshotLinks(sequences).count();
        int duplicateMarkers = locators.duplicateScreenshotMarkers(sequences).count();
        int nestedImages = locators.nestedScreenshotImages(sequences).count();
        int directLinks = locators.dataUriScreenshotLinks(sequences).count();
        int featherlightLinks = locators.featherlightScreenshotLinks(sequences).count();
        Locator anchors = locators.base64ScreenshotLinks(sequences);
        Locator actionRows = locators.actionRows(sequences);
        String actionText = actionRows.count() > 0
                ? actionRows.first().innerText().replace("\n", " | ") : "no action rows";
        boolean structured = actionRows.count() > 0
                && locators.actionRowCells(actionRows.first()).count()
                == ReportUiContract.ACTION_TABLE_COLUMN_COUNT
                && !actionText.contains("ASSERTIONEXPECTEDACTUALRESULT");
        int canonicalLinks = 0;
        for (int index = 0; index < anchors.count(); index++) {
            if (anchors.nth(index).innerText().trim().contains("📷 base64 img")) canonicalLinks++;
        }
        AtfLocator validationTable = new AtfLocator(validations.first(), ReportUiContract.VALIDATION_TABLE_SELECTOR);
        auditFlow.actions().findElement(validationTable);
        auditFlow.actions().expectText(validationTable, "ASSERTION");
        Locator visibleDisclosure = locators.visibleDisclosure(detail);
        if (visibleDisclosure.count() > 0 && !"true".equals(visibleDisclosure.getAttribute("open"))) {
            auditFlow.actions().click(new AtfLocator(locators.disclosureSummary(visibleDisclosure),
                    "details:visible > summary"), "screen evidence is expanded");
        }
        return new ReportAuditSnapshot.VisualEvidence(validations.count(), disclosures.count(), sequences.count(),
                screenshotLinks, duplicateMarkers, nestedImages, directLinks, featherlightLinks,
                anchors.count(), canonicalLinks, actionText, structured, structureAuditor.hasResponsiveActionTableCss());
    }

    public ReportAuditSnapshot.ScreenshotEvidence extractActionableScreenshot(Page page) {
        auditFlow.actions().click(locators.firstTestOwnerAction(page), "class detail opens");
        Locator detail = locators.visibleTestDetail(page);
        Locator sequence = null;
        Locator scenarios = locators.scenarios(detail);
        for (int scenarioIndex = 0; scenarioIndex < scenarios.count() && sequence == null; scenarioIndex++) {
            Locator scenario = scenarios.nth(scenarioIndex);
            if (!"true".equals(scenario.getAttribute("data-atf-scenario-open"))) {
                auditFlow.actions().click(locators.scenarioNodeAction(scenario), "scenario evidence expands");
            }
            Locator sequences = locators.actionSequences(scenario);
            for (Locator candidate : sequences.all()) {
                for (Locator ancestor : locators.nodeAncestors(candidate).all()) {
                    Locator header = locators.nodeHeader(ancestor);
                    Locator node = locators.nodeControl(header);
                    if (node.count() == 0 || !node.isVisible()) continue;
                    String role = ancestor.getAttribute("data-atf-node-role");
                    String openAttribute = "scenario".equals(role) ? "data-atf-scenario-open"
                            : "phase".equals(role) ? "data-atf-phase-open" : "aria-expanded";
                    if (!"true".equals(ancestor.getAttribute(openAttribute))) {
                        auditFlow.actions().click(locators.nodeHeaderAction(header), role + " evidence expands");
                    }
                }
                if (candidate.isVisible() && locators.base64ScreenshotLinks(candidate).count() > 0) {
                    sequence = candidate;
                    break;
                }
            }
        }
        if (sequence == null || !sequence.isVisible()) return ReportAuditSnapshot.ScreenshotEvidence.notAvailable();
        if (!"true".equals(sequence.getAttribute("open"))) {
            auditFlow.actions().click(new AtfLocator(locators.sequenceSummary(sequence),
                    ReportUiContract.ACTION_SEQUENCE_SELECTOR + " > summary"), "action sequence expands");
        }
        Locator link = locators.base64ScreenshotLinks(sequence).first();
        String href = link.getAttribute("href");
        String target = link.getAttribute("target");
        String rel = link.getAttribute("rel");
        String featherlight = link.getAttribute("data-featherlight");
        String marker = link.innerText();
        int inlineImages = locators.inlineScreenshotImages(link).count();
        Set<String> payloads = new HashSet<>();
        for (Locator candidate : locators.base64ScreenshotLinks(sequence).all()) {
            String candidateHref = candidate.getAttribute("href");
            if (candidateHref != null && !candidateHref.isBlank()) payloads.add(candidateHref);
        }
        String geometry = link.evaluate("""
                element => {
                    const row = element.closest('.atf-ui-action-screenshot');
                    if (!row) return 'missing-row';
                    const linkRect = element.getBoundingClientRect();
                    const rowRect = row.getBoundingClientRect();
                    const linkStyle = getComputedStyle(element);
                    const rowStyle = getComputedStyle(row);
                    return [linkRect.left - rowRect.left,
                        (linkRect.top + linkRect.height / 2) - (rowRect.top + rowRect.height / 2),
                        linkStyle.alignSelf, linkStyle.verticalAlign, linkStyle.transform,
                        rowStyle.alignItems, rowStyle.verticalAlign, rowStyle.overflow,
                        rowStyle.overflowY].join('|');
                }
                """).toString();
        String[] parts = geometry.split("\\|", -1);
        boolean centered = parts.length == 9 && closeToZero(parts[0], 1.5) && closeToZero(parts[1], 1.5)
                && "center".equals(parts[2]) && "middle".equals(parts[3]) && "none".equals(parts[4])
                && "center".equals(parts[5]) && "middle".equals(parts[6])
                && "visible".equals(parts[7]) && "visible".equals(parts[8]);
        auditFlow.actions().click(new AtfLocator(link, ReportUiContract.SCREENSHOT_LINK_SELECTOR),
                "Base64 image overlay opens");
        Locator lightbox = locators.lightbox(page);
        Locator lightboxImage = locators.lightboxImage(lightbox);
        String lightboxSource = lightboxImage.getAttribute("src");
        boolean opened = lightbox.count() == 1 && lightboxImage.count() == 1 && isPngDataUri(lightboxSource);
        boolean actionable = link.isVisible() && isPngDataUri(href) && "_blank".equals(target)
                && rel != null && rel.contains("noopener") && "image".equals(featherlight)
                && payloads.size() == 1 && marker.trim().contains("📷 base64 img") && centered && opened;
        if (lightbox.count() == 1) {
            auditFlow.actions().click(new AtfLocator(locators.lightboxClose(lightbox), ".featherlight-close-icon"),
                    "Base64 image overlay closes");
        }
        return new ReportAuditSnapshot.ScreenshotEvidence(actionable, isPngDataUri(href), target, rel,
                featherlight, marker, geometry, centered, opened, payloads.size(), inlineImages);
    }

    private static boolean closeToZero(String value, double tolerance) {
        try { return Math.abs(Double.parseDouble(value)) <= tolerance; }
        catch (NumberFormatException ignored) { return false; }
    }

    private static boolean isPngDataUri(String href) {
        String prefix = "data:image/png;base64,";
        if (href == null || !href.startsWith(prefix) || href.length() == prefix.length()) return false;
        try {
            byte[] bytes = Base64.getDecoder().decode(href.substring(prefix.length()));
            return bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50
                    && bytes[2] == 0x4E && bytes[3] == 0x47 && bytes[4] == 0x0D
                    && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A;
        } catch (IllegalArgumentException ignored) { return false; }
    }
}
