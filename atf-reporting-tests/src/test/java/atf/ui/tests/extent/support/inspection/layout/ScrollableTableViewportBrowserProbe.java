package atf.ui.tests.extent.support.inspection.layout;

import atf.reporting.ui.contract.ReportUiContract;
import atf.reporting.theme.tokens.AtfReportThemeTokens;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.tests.extent.support.api.ApiReportUiFlow;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Measures the published table viewport contract at standard and narrow CSS viewports. */
public final class ScrollableTableViewportBrowserProbe {
    public static final int NARROW_VIEWPORT_WIDTH = 591;
    public static final int SHARED_SCROLL_CAP_PX = Integer.parseInt(
            AtfReportThemeTokens.TABLE_SCROLL_HEIGHT.replace("px", ""));
    public static final int NESTED_AGGREGATE_SUMMARY_HEIGHT_PX = 34;
    private static final String UI_FIXTURE_OWNER = "ScrollableTableFixture";
    private static final String API_FIXTURE_OWNER = "ScrollableApiAggregateFixture";
    private static final double GEOMETRY_TOLERANCE_PX = 1.0;
    private static final double SCROLL_END_TOLERANCE_PX = 2.0;
    private static final String MEASURE_SCRIPT = """
            (selectors) => {
              const visible = element => element.getClientRects().length > 0
                && getComputedStyle(element).display !== 'none';
              const surfaces = selectors.flatMap(({name, selector}) =>
                Array.from(document.querySelectorAll(selector)).filter(visible).map((element, index) => {
                  const style = getComputedStyle(element);
                  const table = element.querySelector('table');
                  const header = table && table.querySelector('thead th');
                  const before = header ? header.getBoundingClientRect().top : null;
                  const maxHeight = Number.parseFloat(style.maxHeight);
                  element.scrollTop = element.scrollHeight;
                  void element.offsetHeight;
                  const after = header ? header.getBoundingClientRect().top : null;
                  const top = element.scrollTop;
                  const clientHeight = element.clientHeight;
                  const scrollHeight = element.scrollHeight;
                  element.scrollLeft=element.scrollWidth;
                  const lastColumn=table?.tBodies[0]?.rows[0]?.lastElementChild;
                  const columnsReachable=(!lastColumn || lastColumn.getBoundingClientRect().right
                    <=element.getBoundingClientRect().right+2)
                    && (!table || getComputedStyle(table).display==='table')
                    && (element.scrollWidth<=element.clientWidth+2 || getComputedStyle(element).overflowX==='auto');
                  element.scrollLeft=0;
                  return {
                    name: name + (index ? ' #' + (index + 1) : ''),
                    clientWidth: element.clientWidth,
                    scrollWidth: element.scrollWidth,
                    tableWidth: table ? table.scrollWidth : element.clientWidth,
                    clientHeight,
                    scrollHeight,
                    scrollTop: top,
                    maxHeight,
                    long: scrollHeight > clientHeight + 1,
                    reachesEnd: top + clientHeight >= scrollHeight - %s,
                    hasHeader: !!header,
                    stickyHeader: !header || getComputedStyle(header).position === 'sticky'
                      && Math.abs(after - before) <= 2,
                    columnsReachable
                  };
                }));
              return {
                documentWidth: document.documentElement.clientWidth,
                documentScrollWidth: document.documentElement.scrollWidth,
                surfaces
              };
            }
            """.formatted(SCROLL_END_TOLERANCE_PX);
    private static final String PRESENCE_SCRIPT = """
            selectors => selectors.map(({name, selector}) => {
              const matches = Array.from(document.querySelectorAll(selector));
              const visible = element => element.getClientRects().length > 0
                && getComputedStyle(element).display !== 'none';
              const hiddenReason = element => {
                const reasons = [];
                for (let parent = element; parent && parent !== document.body; parent = parent.parentElement) {
                  const style = getComputedStyle(parent);
                  if (parent.tagName === 'DETAILS') reasons.push('details.open=' + parent.open);
                  if (style.display === 'none' || style.visibility === 'hidden') {
                    reasons.push(parent.tagName.toLowerCase() + '.' + String(parent.className).trim()
                      + ' display=' + style.display + ' visibility=' + style.visibility);
                  }
                }
                return reasons.join(' <- ') || 'no hidden ancestor identified';
              };
              const hidden = matches.filter(element => !visible(element))
                .map(element => hiddenReason(element));
              return name + ': matched=' + matches.length
                + ', visible=' + matches.filter(visible).length
                + (hidden.length ? ', hidden=[' + hidden.join(' | ') + ']' : '');
            })
            """;
    private static final List<NamedSelector> OVERVIEW_SELECTORS = List.of(
            new NamedSelector("Overview categories", ".dashboard-view .category-container > .card > .card-body"),
            new NamedSelector("System/Environment", ".dashboard-view .sysenv-container > .card > .card-body"),
            new NamedSelector("Tag audit", ".atf-tag-scroll"));
    private static final List<NamedSelector> UI_TEST_SELECTORS = List.of(
            new NamedSelector("Playwright actions", ".atf-ui-action-table-wrap"),
            new NamedSelector("Audit/Assertions", ".atf-validation-table-wrap"),
            new NamedSelector("TECHNICAL TRACE", ".atf-trace-table-wrap"));
    private static final List<NamedSelector> API_TEST_SELECTORS = List.of(
            new NamedSelector("Unique request signatures", ".network-signature-list.network-aggregate-scroll"),
            new NamedSelector("Technical diagnostics", ".network-diagnostic-list.network-aggregate-scroll"),
            new NamedSelector("TECHNICAL TRACE", ".atf-trace-table-wrap"));

    public Result inspect(UiAuditFlow uiFlow, UiAuditFlow apiFlow, UiBrowserConfiguration browser,
                          ScrollableTableReportFixture.Density density) {
        Objects.requireNonNull(uiFlow, "uiFlow");
        Objects.requireNonNull(apiFlow, "apiFlow");
        Objects.requireNonNull(browser, "browser");
        Objects.requireNonNull(density, "density");
        Page uiPage = uiFlow.openTab(ReportUiContract.Tab.OVERVIEW.hash());
        ReportUiLocators uiLocators = new ReportUiLocators();
        ApiReportUiFlow uiReportFlow = new ApiReportUiFlow(uiFlow, uiLocators);
        Page apiPage = apiFlow.openTab(ReportUiContract.Tab.TESTS.hash());
        ReportUiLocators apiLocators = new ReportUiLocators();
        ApiReportUiFlow apiReportFlow = new ApiReportUiFlow(apiFlow, apiLocators);
        List<Measurement> measurements = new ArrayList<>();
        List<SummaryGeometry> summaries = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();

        for (Viewport viewport : List.of(
                new Viewport("standard", browser.viewportWidth(), browser.viewportHeight()),
                new Viewport("narrow", NARROW_VIEWPORT_WIDTH, browser.viewportHeight()))) {
            uiPage.setViewportSize(viewport.width(), viewport.height());
            settle(uiPage);
            uiPage = uiFlow.openTabContinuing(ReportUiContract.Tab.TESTS.hash());
            ApiReportUiFlow.Owner uiOwner = uiReportFlow.openOwner(uiPage, UI_FIXTURE_OWNER);
            uiReportFlow.expandAll(uiOwner.detail());
            expandValidationNodes(uiFlow, uiLocators, uiOwner.detail());
            expandDisclosures(uiLocators, uiOwner.detail());

            uiPage = uiFlow.openTabContinuing(ReportUiContract.Tab.OVERVIEW.hash());
            expandDisclosures(uiLocators, uiLocators.tabView(uiPage, ReportUiContract.Tab.OVERVIEW));
            measurements.addAll(measure(uiPage, viewport, "Overview", OVERVIEW_SELECTORS));

            uiPage = uiFlow.openTabContinuing(ReportUiContract.Tab.TESTS.hash());
            uiOwner = uiReportFlow.openOwner(uiPage, UI_FIXTURE_OWNER);
            uiReportFlow.expandAll(uiOwner.detail());
            expandValidationNodes(uiFlow, uiLocators, uiOwner.detail());
            expandDisclosures(uiLocators, uiOwner.detail());
            diagnostics.addAll(surfaceDiagnostics(uiPage, UI_TEST_SELECTORS,
                    viewport.name() + " UI"));
            measurements.addAll(measure(uiPage, viewport, "UI Tests", UI_TEST_SELECTORS));

            apiPage.setViewportSize(viewport.width(), viewport.height());
            settle(apiPage);
            apiPage = apiFlow.openTabContinuing(ReportUiContract.Tab.TESTS.hash());
            ApiReportUiFlow.Owner apiOwner = apiReportFlow.openOwner(apiPage, API_FIXTURE_OWNER);
            apiReportFlow.expandAll(apiOwner.detail());
            recordExpandedRequests(apiReportFlow, apiOwner, viewport.name(), diagnostics);
            summaries.addAll(summaries(apiPage, viewport.name(), viewport.width(), true));
            expandDisclosures(apiLocators, apiOwner.detail());
            diagnostics.addAll(surfaceDiagnostics(apiPage, API_TEST_SELECTORS,
                    viewport.name() + " API"));
            measurements.addAll(measure(apiPage, viewport, "API Tests", API_TEST_SELECTORS));
        }
        return new Result(density.expectsLongContent(), measurements, summaries, diagnostics);
    }

    private static void recordExpandedRequests(ApiReportUiFlow flow, ApiReportUiFlow.Owner owner,
                                               String viewport, List<String> diagnostics) {
        int expandedRequests = flow.expandOwnerRequests(owner);
        diagnostics.add(viewport + " Tests request/evidence cards expanded=" + expandedRequests);
        if (expandedRequests == 0) {
            throw new AssertionError("The fixture network inspector must expose request/evidence cards");
        }
    }

    private static void expandDisclosures(ReportUiLocators locators, Locator scope) {
        locators.allDisclosures(scope).evaluateAll(
                "items => items.forEach(item => { item.open = true; })");
        locators.collapsedReportPanels(scope).evaluateAll(
                "items => items.forEach(item => { item.classList.add('show'); })");
        Page page = scope.page();
        settle(page);
    }

    private static void expandValidationNodes(UiAuditFlow flow, ReportUiLocators locators,
                                              Locator scope) {
        Locator validations = locators.validationNodes(scope);
        for (int index = 0; index < validations.count(); index++) {
            Locator node = validations.nth(index);
            Locator card = locators.cardForNode(node);
            Locator header = locators.nodeHeader(card);
            if (card.isVisible() && header.isVisible()
                    && !"true".equals(header.getAttribute("aria-expanded"))) {
                flow.actions().click(locators.nodeHeaderAction(header),
                        "UI validation table rows become visible");
            }
        }
    }

    private static List<SummaryGeometry> summaries(Page page, String viewport,
                                                    int viewportWidth, boolean defaultState) {
        Object raw = page.evaluate("() => Array.from(document.querySelectorAll("
                + "'details.network-signatures,details.network-diagnostics'))"
                + ".filter(details => details.getClientRects().length > 0).map(details => {"
                + "const summary=details.querySelector(':scope > summary');"
                + "return {kind:details.classList.contains('network-signatures')?'signatures':'diagnostics',"
                + "height:Number.parseFloat(getComputedStyle(summary).height),open:details.open};})");
        if (!(raw instanceof List<?> list)) return List.of();
        return list.stream().filter(Map.class::isInstance).map(Map.class::cast)
                .map(value -> new SummaryGeometry(viewport, viewportWidth,
                        String.valueOf(value.get("kind")), number(value.get("height")),
                        Boolean.TRUE.equals(value.get("open")), defaultState))
                .toList();
    }

    private static List<Measurement> measure(Page page, Viewport viewport, String view,
                                             List<NamedSelector> selectors) {
        List<Map<String, String>> arguments = selectors.stream()
                .map(selector -> Map.of("name", selector.name(), "selector", selector.selector()))
                .toList();
        Object raw = page.evaluate(MEASURE_SCRIPT, arguments);
        if (!(raw instanceof Map<?, ?> result)) return List.of();
        Object surfacesRaw = result.get("surfaces");
        if (!(surfacesRaw instanceof List<?> surfaces)) return List.of();
        boolean documentFits = number(result.get("documentScrollWidth"))
                <= number(result.get("documentWidth")) + GEOMETRY_TOLERANCE_PX;
        return surfaces.stream().filter(Map.class::isInstance).map(Map.class::cast)
                .map(value -> new Measurement(viewport.name(), viewport.width(), view,
                        String.valueOf(value.get("name")),
                        number(value.get("clientWidth")), number(value.get("scrollWidth")),
                        number(value.get("tableWidth")), number(value.get("clientHeight")),
                        number(value.get("scrollHeight")), number(value.get("scrollTop")),
                        number(value.get("maxHeight")), Boolean.TRUE.equals(value.get("long")),
                        Boolean.TRUE.equals(value.get("reachesEnd")),
                        Boolean.TRUE.equals(value.get("hasHeader")),
                        Boolean.TRUE.equals(value.get("stickyHeader")),
                        Boolean.TRUE.equals(value.get("columnsReachable")), documentFits))
                .toList();
    }

    private static List<String> surfaceDiagnostics(Page page, List<NamedSelector> selectors,
                                                   String viewport) {
        List<Map<String, String>> arguments = selectors.stream()
                .map(selector -> Map.of("name", selector.name(), "selector", selector.selector()))
                .toList();
        Object raw = page.evaluate(PRESENCE_SCRIPT, arguments);
        if (!(raw instanceof List<?> values)) return List.of(viewport + " Tests: diagnostics unavailable");
        return values.stream().map(value -> viewport + " Tests " + value).toList();
    }

    private static void settle(Page page) {
        page.evaluate("() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))");
    }

    private static double number(Object value) {
        return value instanceof Number number ? number.doubleValue() : Double.NaN;
    }

    private record NamedSelector(String name, String selector) { }

    private record Viewport(String name, int width, int height) { }

    public record SummaryGeometry(String viewport, int viewportWidth, String kind,
                                  double heightPx, boolean open, boolean defaultState) {
        public boolean valid() {
            return ("signatures".equals(kind) || "diagnostics".equals(kind))
                    && Math.abs(heightPx - NESTED_AGGREGATE_SUMMARY_HEIGHT_PX)
                    <= GEOMETRY_TOLERANCE_PX
                    && (!defaultState || !open);
        }
    }

    public record Measurement(String viewport, int viewportWidth, String view, String surface,
                              double clientWidth, double scrollWidth, double tableWidth,
                              double clientHeight, double scrollHeight, double scrollTopAtEnd,
                              double maxHeight, boolean longContent, boolean reachesEnd,
                              boolean hasHeader, boolean stickyHeader, boolean columnsReachable,
                              boolean documentFits) {
        public boolean valid() {
            boolean horizontalFit = columnsReachable && documentFits;
            boolean sharedCap = Math.abs(maxHeight - SHARED_SCROLL_CAP_PX) <= GEOMETRY_TOLERANCE_PX
                    && clientHeight <= SHARED_SCROLL_CAP_PX + GEOMETRY_TOLERANCE_PX;
            boolean shortContentIsNatural = longContent
                    || clientHeight < SHARED_SCROLL_CAP_PX - GEOMETRY_TOLERANCE_PX;
            boolean longContentIsReachable = !longContent
                    || reachesEnd && (!hasHeader || stickyHeader);
            return horizontalFit && sharedCap && shortContentIsNatural && longContentIsReachable;
        }

        public String summary() {
            return "%s/%dpx %s: client=%sx%s scroll=%sx%s scrollTop=%s maxHeight=%s long=%s sticky=%s"
                    .formatted(viewport, viewportWidth, surface, clientWidth, clientHeight,
                            scrollWidth, scrollHeight, scrollTopAtEnd, maxHeight,
                            longContent, stickyHeader);
        }
    }

    public record Result(boolean expectsLongContent, List<Measurement> measurements,
                         List<SummaryGeometry> aggregateSummaries, List<String> diagnostics) {
        public Result {
            measurements = List.copyOf(Objects.requireNonNull(measurements, "measurements"));
            aggregateSummaries = List.copyOf(Objects.requireNonNull(aggregateSummaries,
                    "aggregateSummaries"));
            diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        }

        public boolean valid() {
            if (measurements.isEmpty() || aggregateSummaries.size() != 4
                    || aggregateSummaries.stream().anyMatch(summary -> !summary.valid())) return false;
            if (measurements.stream().anyMatch(measurement -> !measurement.valid())) return false;
            List<String> expectedSurfaces = List.of("Overview categories", "System/Environment",
                    "Tag audit", "Playwright actions", "Audit/Assertions",
                    "TECHNICAL TRACE", "Unique request signatures", "Technical diagnostics");
            if (expectedSurfaces.stream().anyMatch(expected -> measurements.stream()
                    .noneMatch(measurement -> measurement.surface().startsWith(expected)))) return false;
            for (String viewport : List.of("standard", "narrow")) {
                List<Measurement> atViewport = measurements.stream()
                        .filter(measurement -> measurement.viewport().equals(viewport)).toList();
                if (atViewport.isEmpty()
                        || expectedSurfaces.stream().anyMatch(expected -> atViewport.stream()
                        .noneMatch(measurement -> measurement.surface().startsWith(expected)))
                        || atViewport.stream().noneMatch(measurement -> !measurement.longContent())) {
                    return false;
                }
                List<SummaryGeometry> summariesAtViewport = aggregateSummaries.stream()
                        .filter(summary -> summary.viewport().equals(viewport)).toList();
                if (summariesAtViewport.size() != 2 || summariesAtViewport.stream()
                        .map(SummaryGeometry::kind).distinct().count() != 2) return false;
                boolean hasLongContent = atViewport.stream().anyMatch(Measurement::longContent);
                if (expectsLongContent != hasLongContent) return false;
                List<Measurement> traces = atViewport.stream()
                        .filter(measurement -> measurement.surface().startsWith("TECHNICAL TRACE"))
                        .toList();
                if (traces.isEmpty() || (expectsLongContent
                        ? traces.stream().noneMatch(Measurement::longContent)
                        : traces.stream().anyMatch(Measurement::longContent))) return false;
            }
            return true;
        }

        public List<String> violations() {
            List<String> violations = new ArrayList<>();
            measurements.stream().filter(measurement -> !measurement.valid())
                    .map(Measurement::summary).forEach(violations::add);
            aggregateSummaries.stream().filter(summary -> !summary.valid())
                    .map(summary -> "aggregate summary: " + summary).forEach(violations::add);
            if (measurements.isEmpty()) violations.add("no visible scroll viewport was measured");
            List<String> expectedSurfaces = List.of("Overview categories", "System/Environment",
                    "Tag audit", "Playwright actions", "Audit/Assertions",
                    "TECHNICAL TRACE", "Unique request signatures", "Technical diagnostics");
            for (String viewport : List.of("standard", "narrow")) {
                List<Measurement> atViewport = measurements.stream()
                        .filter(measurement -> measurement.viewport().equals(viewport)).toList();
                expectedSurfaces.stream().filter(expected -> atViewport.stream()
                        .noneMatch(measurement -> measurement.surface().startsWith(expected)))
                        .map(expected -> "missing " + viewport + " surface: " + expected)
                        .forEach(violations::add);
                List<Measurement> traces = atViewport.stream()
                        .filter(measurement -> measurement.surface().startsWith("TECHNICAL TRACE"))
                        .toList();
                if (traces.isEmpty()) {
                    violations.add("missing " + viewport + " TECHNICAL TRACE viewport");
                } else if (expectsLongContent && traces.stream().noneMatch(Measurement::longContent)) {
                    violations.add("long " + viewport + " TECHNICAL TRACE did not overflow into its own scroll viewport");
                } else if (!expectsLongContent && traces.stream().anyMatch(Measurement::longContent)) {
                    violations.add("short " + viewport + " TECHNICAL TRACE reserved a scroll viewport");
                }
                List<SummaryGeometry> summariesAtViewport = aggregateSummaries.stream()
                        .filter(summary -> summary.viewport().equals(viewport)).toList();
                if (summariesAtViewport.size() != 2) {
                    violations.add("expected 2 visible aggregate summaries at " + viewport
                            + " viewport but measured " + summariesAtViewport.size());
                }
            }
            return List.copyOf(violations);
        }

        public String summary() {
            return "measurements=" + measurements.size() + " "
                    + measurements.stream().map(Measurement::summary).toList()
                    + "; aggregate summaries="
                    + aggregateSummaries + "; diagnostics=" + diagnostics
                    + "; violations=" + violations();
        }
    }
}
