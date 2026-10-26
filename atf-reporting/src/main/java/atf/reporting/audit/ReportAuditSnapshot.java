package atf.reporting.audit;
import atf.reporting.api.evidence.ReportBodyAvailability;

import atf.reporting.contract.AtfReportStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Renderer-neutral observations produced by the Extent report inspectors.
 *
 * <p>The records contain facts observed from a report, not Playwright
 * locators, navigation actions or JUnit assertions. A browser adapter may
 * attach a page to a flow-specific envelope, while validators and other
 * publishers consume these immutable observations unchanged.</p>
 */
public final class ReportAuditSnapshot {
    private ReportAuditSnapshot() { }

    /** Renderer-neutral Overview facts; no browser object crosses this boundary. */
    public record Overview(int sectionCount, int requiredSectionCount,
                           int classCount, int scenarioCount,
                           int auditScenarioTotal, int auditRowCount,
                           boolean complete, List<String> violations) {
        public Overview {
            if (sectionCount < 0 || requiredSectionCount < 0 || classCount < 0
                    || scenarioCount < 0 || auditScenarioTotal < 0 || auditRowCount < 0) {
                throw new IllegalArgumentException("Overview snapshot counts must be non-negative");
            }
            violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
        }
    }

    public record Tests(int classCount, int scenarioCount, int phaseCount,
                        int requestCount, int validationCount,
                        List<String> classNames, List<String> scenarioTitles,
                        List<AtfReportStatus> scenarioStatuses) {
        public Tests(int classCount, int scenarioCount, int phaseCount,
                     int requestCount, int validationCount, List<String> classNames) {
            this(classCount, scenarioCount, phaseCount, requestCount, validationCount,
                    classNames, List.of(), List.of());
        }

        public Tests(int classCount, int scenarioCount, int phaseCount,
                     int requestCount, int validationCount, List<String> classNames,
                     List<String> scenarioTitles) {
            this(classCount, scenarioCount, phaseCount, requestCount, validationCount,
                    classNames, scenarioTitles, List.of());
        }

        public Tests {
            if (classCount < 0 || scenarioCount < 0 || phaseCount < 0
                    || requestCount < 0 || validationCount < 0) {
                throw new IllegalArgumentException("Tests snapshot counts must be non-negative");
            }
            classNames = List.copyOf(Objects.requireNonNull(classNames, "classNames"));
            scenarioTitles = List.copyOf(Objects.requireNonNull(scenarioTitles, "scenarioTitles"));
            scenarioStatuses = List.copyOf(Objects.requireNonNull(scenarioStatuses,
                    "scenarioStatuses"));
            if (!scenarioStatuses.isEmpty() && scenarioStatuses.size() != scenarioCount) {
                throw new IllegalArgumentException(
                        "scenarioStatuses must align one-to-one with rendered scenarios");
            }
        }
    }

    public record Annotations(int categoryCount, int linkedScenarioCount,
                              String filterSurface, boolean styled,
                              List<Integer> linkCardinalities) {
        public Annotations(int categoryCount, int linkedScenarioCount,
                           String filterSurface, boolean styled) {
            this(categoryCount, linkedScenarioCount, filterSurface, styled,
                    linkedScenarioCount == 0 ? List.of() : List.of(linkedScenarioCount));
        }

        public Annotations {
            if (categoryCount < 0 || linkedScenarioCount < 0) {
                throw new IllegalArgumentException("Annotation counts must be non-negative");
            }
            filterSurface = Objects.requireNonNull(filterSurface, "filterSurface");
            linkCardinalities = List.copyOf(Objects.requireNonNull(linkCardinalities,
                    "linkCardinalities"));
            if (linkCardinalities.stream().anyMatch(value -> value == null || value < 0)) {
                throw new IllegalArgumentException("Category link cardinalities must be non-negative");
            }
            if (linkCardinalities.size() > categoryCount) {
                throw new IllegalArgumentException("Cannot record more category cardinalities than categories");
            }
        }

        public Cardinality cardinality() {
            return categoryCount == 0 ? Cardinality.ZERO
                    : categoryCount == 1 ? Cardinality.ONE : Cardinality.MANY;
        }

        public boolean hasConsistentLinkCardinality() {
            return switch (cardinality()) {
                case ZERO -> linkedScenarioCount == 0;
                case ONE, MANY -> linkedScenarioCount >= 0;
            };
        }

        public boolean hasZeroLinks() {
            return linkCardinalities.stream().anyMatch(value -> value == 0);
        }

        public boolean hasOneLink() {
            return linkCardinalities.stream().anyMatch(value -> value == 1);
        }

        public boolean hasManyLinks() {
            return linkCardinalities.stream().anyMatch(value -> value > 1);
        }
    }

    public enum Cardinality { ZERO, ONE, MANY }

    public record Failures(int failureCount, int primaryCauseCount,
                           int openLinkCount, boolean emptyState) {
        public Failures {
            if (failureCount < 0 || primaryCauseCount < 0 || openLinkCount < 0) {
                throw new IllegalArgumentException("Failure counts must be non-negative");
            }
        }
    }

    public record RequestEvidence(boolean tablesVisible, int requestTableCount,
                                  int responseTableCount, int executionTableCount,
                                  JsonEvidence jsonEvidence) {
        public RequestEvidence {
            if (requestTableCount < 0 || responseTableCount < 0 || executionTableCount < 0) {
                throw new IllegalArgumentException("Evidence counts must be non-negative");
            }
            jsonEvidence = Objects.requireNonNull(jsonEvidence, "jsonEvidence");
        }
    }

    /** Renderer-neutral facts for the legacy visual-evidence contract. */
    public record VisualEvidence(int validationCount, int disclosureCount,
                                 int actionSequenceCount, int screenshotLinkCount,
                                 int duplicateScreenshotMarkers, int nestedScreenshotImages,
                                 int directScreenshotLinks, int featherlightScreenshotLinks,
                                 int screenshotAnchorCount, int canonicalScreenshotLinks,
                                 String observedActionText, boolean observedTextIsStructured,
                                 boolean responsiveActionTable) {
        public VisualEvidence {
            if (validationCount < 0 || disclosureCount < 0 || actionSequenceCount < 0
                    || screenshotLinkCount < 0 || duplicateScreenshotMarkers < 0
                    || nestedScreenshotImages < 0 || directScreenshotLinks < 0
                    || featherlightScreenshotLinks < 0 || screenshotAnchorCount < 0
                    || canonicalScreenshotLinks < 0) {
                throw new IllegalArgumentException("Visual evidence counts must be non-negative");
            }
            observedActionText = Objects.requireNonNull(observedActionText, "observedActionText");
        }
    }

    /** Renderer-neutral facts for the actionable Base64 screenshot contract. */
    public record ScreenshotEvidence(boolean actionable, boolean validPngPayload, String target,
                                     String rel, String featherlight, String markerText,
                                     String geometry, boolean centeredVertically,
                                     boolean openedBase64Image, int distinctPayloadCount,
                                     int inlineImageCount) {
        public ScreenshotEvidence {
            target = Objects.requireNonNull(target, "target");
            rel = Objects.requireNonNull(rel, "rel");
            featherlight = Objects.requireNonNull(featherlight, "featherlight");
            markerText = Objects.requireNonNull(markerText, "markerText");
            geometry = Objects.requireNonNull(geometry, "geometry");
            if (distinctPayloadCount < 0 || inlineImageCount < 0) {
                throw new IllegalArgumentException("Screenshot evidence counts must be non-negative");
            }
        }

        public static ScreenshotEvidence notAvailable() {
            return new ScreenshotEvidence(false, false, "", "", "", "", "missing",
                    false, false, 0, 0);
        }
    }

    /** Neutral result of category/failure filter discovery; pages stay in the browser envelope. */
    public record Filters(Annotations annotations, Failures failures,
                          boolean tagLinks, boolean failureLinks) {
        public Filters {
            annotations = Objects.requireNonNull(annotations, "annotations");
            failures = Objects.requireNonNull(failures, "failures");
        }
    }

    public record JsonEvidence(String requestBody, String responseBody,
                               List<String> violations,
                               ReportBodyAvailability requestAvailability,
                               ReportBodyAvailability responseAvailability) {
        /** Legacy documents prove captured bytes, never confirmed absence. */
        public JsonEvidence(String requestBody, String responseBody, List<String> violations) {
            this(requestBody, responseBody, violations, documentAvailability(requestBody),
                    documentAvailability(responseBody));
        }

        public JsonEvidence {
            requestBody = requestBody == null ? "" : requestBody;
            responseBody = responseBody == null ? "" : responseBody;
            violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
            Objects.requireNonNull(requestAvailability, "requestAvailability");
            Objects.requireNonNull(responseAvailability, "responseAvailability");
        }

        public boolean compliant() {
            return explained(requestBody, requestAvailability)
                    && explained(responseBody, responseAvailability) && violations.isEmpty();
        }

        public boolean requestHasPayload() { return requestAvailability ==
                ReportBodyAvailability.AVAILABLE && hasDocument(requestBody); }

        public boolean responseHasPayload() { return responseAvailability ==
                ReportBodyAvailability.AVAILABLE && hasDocument(responseBody); }

        private static boolean hasDocument(String body) {
            return body != null && !body.isBlank();
        }

        private static boolean explained(String body, ReportBodyAvailability availability) {
            return availability == ReportBodyAvailability.ABSENT
                    ? body.isEmpty() : availability == ReportBodyAvailability.AVAILABLE
                    && hasDocument(body);
        }

        private static ReportBodyAvailability documentAvailability(String body) {
            return body == null || body.isBlank()
                    ? ReportBodyAvailability.NOT_CAPTURED
                    : ReportBodyAvailability.AVAILABLE;
        }
    }

    /** Renderer-neutral contract for one UI validation evidence card. */
    public record UiValidation(int sectionCount, int actionSequenceCount,
                                   int observationCount, int assertionTableCount,
                                   int zoneRowCount, int screenshotLinkCount,
                                   int canonicalScreenshotLinkCount,
                                   boolean zonesAreDirectSiblings,
                                   boolean zonesStacked, boolean noEmptyRows,
                                   boolean noDecorativeSeparators, boolean inlineScreenshot,
                                   List<String> violations) {
        /** Backward-compatible bridge for the pre-section-module evidence contract. */
        public UiValidation(int actionZoneCount, int observationZoneCount,
                                int assertionZoneCount, int zoneRowCount,
                                int horizontalRuleCount, boolean zonesShareEventTable,
                                boolean zonesAreContiguous, boolean inlineScreenshotLink,
                                boolean screenshotVerticallyCentered,
                                boolean screenshotOpened) {
            this(1, actionZoneCount, observationZoneCount, assertionZoneCount, zoneRowCount,
                    inlineScreenshotLink ? 1 : 0, inlineScreenshotLink ? 1 : 0,
                    zonesShareEventTable, zonesAreContiguous, zoneRowCount == 3,
                    horizontalRuleCount == 0, inlineScreenshotLink,
                    screenshotOpened ? List.of() : List.of("screenshot was not opened"));
        }

        public UiValidation {
            if (sectionCount < 0 || actionSequenceCount < 0 || observationCount < 0
                    || assertionTableCount < 0 || zoneRowCount < 0
                    || screenshotLinkCount < 0 || canonicalScreenshotLinkCount < 0) {
                throw new IllegalArgumentException("UI validation counts must be non-negative");
            }
            violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
        }

        public boolean valid() {
            return violations.isEmpty();
        }
    }

    public record Duration(int ownerCount, int nodeCount, int durationMarkerCount,
                           int formattedBadgeCount, int aggregatedNodeCount,
                           List<String> violations) {
        public Duration {
            if (ownerCount < 0 || nodeCount < 0 || durationMarkerCount < 0
                    || formattedBadgeCount < 0 || aggregatedNodeCount < 0) {
                throw new IllegalArgumentException("Duration counts must be non-negative");
            }
            violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
        }

        public Duration plus(Duration other) {
            Objects.requireNonNull(other, "other");
            List<String> combined = new ArrayList<>(violations);
            combined.addAll(other.violations);
            return new Duration(Math.max(ownerCount, other.ownerCount),
                    nodeCount + other.nodeCount,
                    durationMarkerCount + other.durationMarkerCount,
                    formattedBadgeCount + other.formattedBadgeCount,
                    aggregatedNodeCount + other.aggregatedNodeCount,
                    combined);
        }
    }

    public record DurationProjection(String projection, int fieldCount, int validCount,
                                     List<String> violations) {
        public DurationProjection {
            projection = Objects.requireNonNull(projection, "projection");
            if (fieldCount < 0 || validCount < 0) {
                throw new IllegalArgumentException("duration projection counts must be non-negative");
            }
            violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
        }

        public boolean valid() { return violations.isEmpty() && validCount == fieldCount; }
    }

    public record BadgeGeometry(Badge normal, Badge narrow) {
        public BadgeGeometry {
            normal = Objects.requireNonNull(normal, "normal");
            narrow = Objects.requireNonNull(narrow, "narrow");
        }
    }

    public record Badge(int count, String labels, String signature) {
        public Badge {
            if (count < 0) throw new IllegalArgumentException("Badge count must be non-negative");
            labels = Objects.requireNonNull(labels, "labels");
            signature = Objects.requireNonNull(signature, "signature");
        }
    }
}
