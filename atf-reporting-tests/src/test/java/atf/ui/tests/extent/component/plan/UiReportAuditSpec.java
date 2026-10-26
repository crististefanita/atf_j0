package atf.ui.tests.extent.component.plan;
import atf.reporting.ui.audit.plan.SectionAuditSpec;
import atf.ui.tests.extent.support.api.ApiReportAuditSetup;
import atf.reporting.ui.audit.vocabulary.UiAuditGherkin;
import atf.ui.tests.extent.support.data.UiTestData;

import atf.ui.tests.extent.support.config.UiConfiguration;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Compatibility lifecycle input for the API-report fixture.
 *
 * <p>Section selection belongs to {@link SectionAuditSpec}.
 * This record only carries report-session metadata needed by the two approved
 * API-to-UI contract profiles and the golden exploratory wiring.</p>
 */
public record UiReportAuditSpec(String id, List<String> apiFamilies, List<String> tags,
                                String setupDescription, String failureTitle,
                                String failureTarget) {
    public static final String SUITE_PROPERTY = "atf.ui.audit.suite";

    public UiReportAuditSpec {
        id = required(id, "id");
        apiFamilies = normalizeFamilies(apiFamilies);
        if (apiFamilies.isEmpty()) {
            throw new IllegalArgumentException("apiFamilies must not be empty");
        }
        tags = List.copyOf(Objects.requireNonNull(tags, "tags"));
        setupDescription = required(setupDescription, "setupDescription");
        failureTitle = required(failureTitle, "failureTitle");
        failureTarget = required(failureTarget, "failureTarget");
    }

    /** Resolves only session metadata; section checks are annotation-driven. */
    public static UiReportAuditSpec resolve(Class<?> owner, UiConfiguration configuration) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(configuration, "configuration");
        String requestedId = System.getProperty(SUITE_PROPERTY);
        String id = requestedId == null || requestedId.isBlank()
                ? owner.getSimpleName().replaceAll("(?<!^)([A-Z])", "-$1")
                    .toLowerCase(Locale.ROOT)
                : requestedId.trim();
        List<String> families = ApiReportAuditSetup.families(owner, configuration);
        List<String> tags = new java.util.ArrayList<>(UiTestData.defaultTags());
        tags.addAll(families);
        return new UiReportAuditSpec(id, families, tags,
                UiAuditGherkin.GENERATED_API_REPORT_READY,
                "UI Extent audit failed", "generated API report");
    }

    public static UiReportAuditSpec legacy(String id, Collection<String> families,
                                           Collection<String> tags,
                                           String setupDescription, String failureTitle,
                                           String failureTarget) {
        List<String> resolvedTags = tags == null ? UiTestData.defaultTags() : List.copyOf(tags);
        return new UiReportAuditSpec(id, normalizeFamilies(families), resolvedTags,
                setupDescription, failureTitle, failureTarget);
    }

    private static List<String> normalizeFamilies(Collection<String> values) {
        if (values == null) return List.of();
        return values.stream().filter(Objects::nonNull)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank()).distinct().toList();
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
