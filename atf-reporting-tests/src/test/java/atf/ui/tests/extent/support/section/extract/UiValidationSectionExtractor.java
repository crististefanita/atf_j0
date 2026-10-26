package atf.ui.tests.extent.support.section.extract;
import atf.reporting.ui.contract.ReportUiContract;
import atf.reporting.theme.tokens.AtfReportThemeTokens;
import atf.ui.playwright.AtfLocator;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;

import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.ui.tests.extent.support.inspection.layout.ValidationTableLayoutInspector;
import atf.reporting.audit.ReportAuditSnapshot;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Browser-only adapter for UI validation. It observes the
 * rendered report and returns a renderer-neutral snapshot; it never asserts.
 */
public final class UiValidationSectionExtractor {
    private static final double SUMMARY_GEOMETRY_TOLERANCE_PX = 1.5;
    private final UiAuditFlow audit;
    private final ReportUiLocators locators;
    private final ValidationTableLayoutInspector validationTableLayout = new ValidationTableLayoutInspector();

    public UiValidationSectionExtractor(UiAuditFlow audit, ReportUiLocators locators) {
        this.audit = Objects.requireNonNull(audit, "audit");
        this.locators = Objects.requireNonNull(locators, "locators");
    }

    public ReportAuditSnapshot.UiValidation inspect(Page page) {
        Objects.requireNonNull(page, "page");
        Locator allNodes = locators.uiValidationNodes(page);
        expandUiValidationAncestors(allNodes);
        allNodes = locators.uiValidationNodes(page);
        int visibleNodes = 0;
        int visibleNodesWithScreenshot = 0;
        int maxVisibleScreenshotLinks = 0;
        for (Locator candidate : allNodes.all()) {
            if (!candidate.isVisible()) continue;
            visibleNodes++;
            int screenshotLinks = locators.uiValidationScreenshotLinks(
                    locators.uiValidationCard(candidate)).count();
            maxVisibleScreenshotLinks = Math.max(maxVisibleScreenshotLinks, screenshotLinks);
            if (screenshotLinks > 0) visibleNodesWithScreenshot++;
        }
        Locator node = locators.visibleUiValidationNodeWithScreenshot(page);
        if (allNodes.count() == 0 || node.count() == 0 || !node.isVisible()) {
            return new ReportAuditSnapshot.UiValidation(0, 0, 0, 0, 0,
                    0, 0, false, false, false, false, false,
                    List.of("visible UI validation node is missing"));
        }

        Locator card = locators.uiValidationCard(node);
        Locator body = card.locator(":scope > .collapse");
        if (!body.isVisible()) {
            audit.actions().click(locators.uiValidationNodeAction(page),
                    "UI validation evidence zones expand");
        }
        Locator rows = locators.uiValidationZoneRows(card);
        int actionSequences = locators.uiValidationActionSequences(card).count();
        int observations = locators.uiValidationObservations(card).count();
        int assertions = locators.uiValidationAssertionTables(card).count();
        List<String> violations = new ArrayList<>();
        validateCompactDisclosures("Playwright actions",
                locators.uiValidationActionSequences(card), violations);
        validateCompactDisclosures("Audit observations",
                locators.uiValidationObservations(card), violations);
        validateCompactDisclosures("Assertions",
                locators.uiValidationAssertionZones(card), violations);
        for (Locator evidenceTable : locators.uiValidationEvidenceTables(card).all()) {
            ValidationTableLayoutInspector.Result layout = validationTableLayout.inspect(evidenceTable);
            if (!layout.valid()) {
                violations.add("Evidence table does not allocate the longest flexible value column dynamically: "
                        + layout.evidence());
            }
        }
        Locator screenshots = locators.uiValidationScreenshotLinks(card);
        Locator sequences = locators.uiValidationActionSequences(card);
        for (int sequenceIndex = 0; sequenceIndex < sequences.count(); sequenceIndex++) {
            Locator sequence = sequences.nth(sequenceIndex);
            if (!"true".equals(sequence.getAttribute("open"))) {
                audit.actions().click(new AtfLocator(
                        locators.sequenceSummary(sequence), ReportUiContract.ACTION_SEQUENCE_SELECTOR
                                + " > summary"), "screen action evidence expands");
            }
        }
        screenshots = locators.uiValidationScreenshotLinks(card);
        // The data-atf-screenshot contract is the canonical machine-readable
        // marker; the label check below protects the user-facing emoji text.
        int canonicalScreenshots = screenshots.count();
        int labeledScreenshots = 0;
        int validPngScreenshots = 0;
        int verticallyCenteredScreenshots = 0;
        for (Locator label : locators.uiValidationScreenshotLabels(card).all()) {
            String text = label.textContent();
            if (text != null && text.contains(ReportUiContract.SCREENSHOT_MARKER)) labeledScreenshots++;
        }
        for (Locator link : screenshots.all()) {
            if (isValidPngEvidence(page, link)) validPngScreenshots++;
            String geometry = link.evaluate("""
                    element => {
                      const row = element.closest('.atf-ui-action-screenshot');
                      if (!row) return 'missing-row';
                      const linkRect = element.getBoundingClientRect();
                      const rowRect = row.getBoundingClientRect();
                      const linkStyle = getComputedStyle(element);
                      const rowStyle = getComputedStyle(row);
                      return [
                        (linkRect.top + linkRect.height / 2) - (rowRect.top + rowRect.height / 2),
                        linkStyle.alignSelf, linkStyle.verticalAlign, linkStyle.transform,
                        rowStyle.alignItems, rowStyle.verticalAlign, rowStyle.overflow,
                        rowStyle.overflowY
                      ].join('|');
                    }
                    """).toString();
            String[] parts = geometry.split("\\|", -1);
            if (parts.length == 8 && closeToZero(parts[0], 1.5)
                    && "center".equals(parts[1]) && "middle".equals(parts[2])
                    && "none".equals(parts[3]) && "center".equals(parts[4])
                    && "middle".equals(parts[5]) && "visible".equals(parts[6])
                    && "visible".equals(parts[7])) {
                verticallyCenteredScreenshots++;
            }
        }

        boolean directSiblings = rows.count() == 3
                && rows.nth(0).locator("details.atf-ui-action-sequence").count() == 1
                && rows.nth(1).locator("details.atf-ui-observations").count() == 1
                && rows.nth(2).locator("table.helm-validation-table").count() == 1;
        boolean semanticActionRows = true;
        for (int sequenceIndex = 0;
             sequenceIndex < locators.uiValidationActionSequences(card).count(); sequenceIndex++) {
            Locator sequence = locators.uiValidationActionSequences(card).nth(sequenceIndex);
            Locator actionRows = locators.actionRows(sequence);
            if (actionRows.count() == 0) semanticActionRows = false;
            for (int rowIndex = 0; rowIndex < actionRows.count(); rowIndex++) {
                Locator row = actionRows.nth(rowIndex);
                Locator identity = locators.actionIdentityCell(row);
            semanticActionRows &= locators.actionRowCells(row).count()
                    == ReportUiContract.ACTION_TABLE_COLUMN_COUNT
                        && identity.count() == 1
                        && locators.actionOperation(identity).count() == 1
                        && locators.actionNumber(identity).count() == 1
                        && locators.actionTime(row).count() == 1;
            }
        }
        boolean noEmptyRows = rows.count() == 3;
        boolean stacked = true;
        boolean noSeparators = true;
        List<Double> tops = new ArrayList<>();
        List<Double> bottoms = new ArrayList<>();
        for (int index = 0; index < rows.count(); index++) {
            Locator row = rows.nth(index);
            String text = row.innerText();
            noEmptyRows &= text != null && !text.isBlank();
            noSeparators &= row.locator(".atf-ui-section-separator, hr").count() == 0;
            var box = row.boundingBox();
            if (box == null) {
                stacked = false;
            } else {
                tops.add(box.y);
                bottoms.add(box.y + box.height);
            }
        }
        for (int index = 1; index < tops.size(); index++) {
            double gap = tops.get(index) - bottoms.get(index - 1);
            stacked &= tops.get(index) >= tops.get(index - 1)
                    && Math.abs(gap) <= SUMMARY_GEOMETRY_TOLERANCE_PX;
        }

        boolean inlineScreenshot = screenshots.count() > 0
                && canonicalScreenshots == screenshots.count()
                && labeledScreenshots == screenshots.count()
                && validPngScreenshots == screenshots.count()
                && verticallyCenteredScreenshots == screenshots.count();
        if (allNodes.count() < 1) violations.add("UI validation node is missing");
        if (actionSequences < 1) {
            violations.add("UI validation must expose at least one Playwright action sequence");
        }
        if (observations < 1) {
            violations.add("UI validation must expose at least one audit observation zone");
        }
        if (assertions < 1) {
            violations.add("UI validation must expose at least one assertion table");
        }
        if (!directSiblings) violations.add("action sequence, observations and assertions are not direct sibling zones");
        if (!semanticActionRows) violations.add("Playwright rows do not expose the dynamic operation/#N identity and semantic time cell");
        if (!stacked) violations.add("UI validation zones are not compactly stacked without vertical gaps");
        if (!noEmptyRows) violations.add("UI validation contains an empty evidence row");
        if (!noSeparators) violations.add("UI validation contains a decorative separator");
        if (screenshots.count() == 0) {
            violations.add("UI validation does not expose a screenshot link; visible nodes="
                    + visibleNodes + ", visible nodes with screenshot="
                    + visibleNodesWithScreenshot + ", max visible screenshot links="
                    + maxVisibleScreenshotLinks + ", node id=" + node.getAttribute("id")
                    + ", card count=" + card.count()
                    + ", action zones=" + locators.uiValidationActionSequences(card).count()
                    + ", direct screenshot links="
                    + card.locator("a.atf-ui-action-screenshot-link").count()
                    + ", page screenshot links="
                    + page.locator("a.atf-ui-action-screenshot-link").count());
        } else {
            if (canonicalScreenshots != screenshots.count()) {
                violations.add("UI validation screenshot links are not all canonical Base64 links: "
                        + canonicalScreenshots + "/" + screenshots.count());
            }
            if (labeledScreenshots != screenshots.count()) {
                violations.add("UI validation screenshot links are missing the "
                        + ReportUiContract.SCREENSHOT_MARKER + " label: "
                        + labeledScreenshots + "/" + screenshots.count());
            }
            if (validPngScreenshots != screenshots.count()) {
                violations.add("UI validation screenshot links do not all contain valid PNG evidence: "
                        + validPngScreenshots + "/" + screenshots.count());
            }
            if (verticallyCenteredScreenshots != screenshots.count()) {
                violations.add("UI validation screenshot links are not vertically centered on the evidence row: "
                        + verticallyCenteredScreenshots + "/" + screenshots.count());
            }
        }
        return new ReportAuditSnapshot.UiValidation(1, actionSequences, observations,
                assertions, rows.count(), screenshots.count(), canonicalScreenshots,
                directSiblings, stacked, noEmptyRows, noSeparators, inlineScreenshot, violations);
    }

    private void validateCompactDisclosures(String label, Locator disclosures,
                                            List<String> violations) {
        if (disclosures.count() == 0) {
            violations.add(label + " disclosure is missing");
            return;
        }
        double expectedHeight = cssPixels(AtfReportThemeTokens.EVIDENCE_SUMMARY_HEIGHT);
        for (int index = 0; index < disclosures.count(); index++) {
            Locator summary = locators.disclosureSummary(disclosures.nth(index));
            if (summary.count() == 0 || !summary.isVisible()) {
                violations.add(label + " disclosure summary is not visible at index " + index);
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> geometry = (Map<String, Object>) summary.evaluate("""
                    element => {
                      const rect = element.getBoundingClientRect();
                      const style = getComputedStyle(element);
                      const rootZoomValue = Number.parseFloat(
                        getComputedStyle(document.documentElement).zoom);
                      const zoom = Number.isFinite(rootZoomValue) && rootZoomValue > 0
                        ? (rootZoomValue > 1 ? rootZoomValue / 100 : rootZoomValue)
                        : 1;
                      const glyph = element.querySelector('.atf-disclosure-glyph');
                      const glyphRect = glyph?.getBoundingClientRect();
                      const markerStyle = glyph
                        ? getComputedStyle(glyph, '::before')
                        : getComputedStyle(element, '::before');
                      const row = element.closest('tr.event-row');
                      const badge = row?.querySelector('td:first-child [class*="badge"]');
                      const badgeRect = badge?.getBoundingClientRect();
                      return {
                        height: rect.height,
                        zoom,
                        padding: style.padding,
                        display: style.display,
                        marker: markerStyle.content,
                        glyphWidth: glyphRect
                          ? glyphRect.width / zoom
                          : Number.parseFloat(getComputedStyle(element, '::before').width),
                        badgeCenterOffset: badgeRect
                          ? (badgeRect.top + badgeRect.height / 2) - (rect.top + rect.height / 2)
                          : null
                      };
                    }
                    """);
            double height = number(geometry, "height");
            double zoom = number(geometry, "zoom");
            if (!Double.isFinite(zoom) || zoom <= 0) zoom = 1;
            double badgeCenterOffset = number(geometry, "badgeCenterOffset");
            String padding = String.valueOf(geometry.get("padding"));
            String marker = String.valueOf(geometry.get("marker"));
            boolean compact = Math.abs(height - expectedHeight * zoom)
                    <= SUMMARY_GEOMETRY_TOLERANCE_PX
                    && AtfReportThemeTokens.EVIDENCE_SUMMARY_PADDING.equals(padding);
            boolean inline = "flex".equals(geometry.get("display"))
                    && (marker.contains("＋") || marker.contains("−"))
                    && Math.abs(number(geometry, "glyphWidth") - 18) <= SUMMARY_GEOMETRY_TOLERANCE_PX;
            boolean badgeAligned = Double.isFinite(badgeCenterOffset)
                    && Math.abs(badgeCenterOffset) <= SUMMARY_GEOMETRY_TOLERANCE_PX;
            if (!compact || !inline || !badgeAligned) {
                violations.add(label + " disclosure differs from compact API evidence geometry: height="
                        + height + ", padding=" + padding + ", display=" + geometry.get("display")
                        + ", zoom=" + zoom + ", glyph=" + marker
                        + ", badge-center-offset=" + badgeCenterOffset);
            }
        }
    }

    private static double cssPixels(String value) {
        return Double.parseDouble(value.replace("px", ""));
    }

    private static double number(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value instanceof Number number ? number.doubleValue() : Double.NaN;
    }

    /**
     * Makes the screen-validation projection independent of which scenario or
     * phase the report renderer initially leaves collapsed. The report contract
     * is about the visible evidence, so its structural ancestors must be open
     * before a screenshot link can be inspected.
     */
    private void expandUiValidationAncestors(Locator nodes) {
        for (Locator node : nodes.all()) {
            List<Locator> ancestors = new ArrayList<>(locators.nodeAncestors(node).all());
            for (Locator ancestor : ancestors) {
                String role = ancestor.getAttribute("data-atf-node-role");
                if (!("scenario".equals(role) || "phase".equals(role)
                        || "request".equals(role) || "validation".equals(role))
                        || !ancestor.isVisible()) {
                    continue;
                }
                String openAttribute = locators.nodeOpenAttribute(ancestor);
                if ("true".equals(ancestor.getAttribute(openAttribute))) continue;
                AtfLocator action = switch (role) {
                    case "scenario" -> locators.scenarioNodeAction(ancestor);
                    case "phase" -> locators.phaseNodeAction(ancestor);
                    default -> locators.requestNodeToggleAction(ancestor);
                };
                audit.actions().click(action, "UI validation structural evidence opens");
            }
        }
    }

    private static boolean closeToZero(String value, double tolerance) {
        try {
            return Math.abs(Double.parseDouble(value)) <= tolerance;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean isPngDataUri(String href) {
        String prefix = "data:image/png;base64,";
        if (href == null || !href.startsWith(prefix) || href.length() == prefix.length()) return false;
        try {
            byte[] bytes = Base64.getDecoder().decode(href.substring(prefix.length()));
            return bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50
                    && bytes[2] == 0x4E && bytes[3] == 0x47 && bytes[4] == 0x0D
                    && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static boolean isValidPngEvidence(Page page, Locator link) {
        if (isPngDataUri(link.getAttribute("href"))) return true;
        String reference = link.getAttribute("data-atf-screenshot-ref");
        if (reference == null || reference.isBlank()) return false;
        Locator canonical = page.locator(
                "a.atf-ui-action-screenshot-link[data-atf-screenshot-ref='" + reference
                        + "'][href^='data:image/png;base64,']");
        return canonical.count() > 0 && isPngDataUri(canonical.first().getAttribute("href"));
    }
}
