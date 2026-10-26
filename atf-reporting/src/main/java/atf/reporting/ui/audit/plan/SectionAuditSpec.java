package atf.reporting.ui.audit.plan;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Immutable, validated interpretation of {@link UiExtentSectionAudit}. */
public record SectionAuditSpec(String id, UiExtentReportSource source,
                               String reportResource, String reportProperty,
                               Set<ExtentSection> sections,
                               Set<ExtentSectionCheck> checks,
                               ExtentJourney journey) {
    public SectionAuditSpec {
        id = required(id, "id");
        source = Objects.requireNonNull(source, "source");
        reportResource = blankToEmpty(reportResource);
        reportProperty = blankToEmpty(reportProperty);
        sections = Set.copyOf(Objects.requireNonNull(sections, "sections"));
        checks = Set.copyOf(Objects.requireNonNull(checks, "checks"));
        journey = Objects.requireNonNull(journey, "journey");
        if (sections.isEmpty()) throw new IllegalArgumentException("sections must not be empty");
        if (checks.isEmpty()) throw new IllegalArgumentException("checks must not be empty");
        validateSource(source, reportResource, reportProperty);
    }

    public static SectionAuditSpec resolve(Class<?> owner) {
        Objects.requireNonNull(owner, "owner");
        UiExtentSectionAudit annotation = owner.getAnnotation(UiExtentSectionAudit.class);
        if (annotation == null) {
            throw new IllegalArgumentException(owner.getName()
                    + " must declare @UiExtentSectionAudit");
        }
        String id = annotation.id().isBlank()
                ? owner.getSimpleName().replaceAll("(?<!^)([A-Z])", "-$1")
                    .toLowerCase(Locale.ROOT)
                : annotation.id().trim();
        return new SectionAuditSpec(id, annotation.source(), annotation.reportResource(),
                annotation.reportProperty(), EnumSet.copyOf(Arrays.asList(annotation.sections())),
                EnumSet.copyOf(Arrays.asList(annotation.checks())), annotation.journey());
    }

    public boolean includes(ExtentSection section) {
        return sections.contains(Objects.requireNonNull(section, "section"));
    }

    /**
     * Makes the declarative check selection readable by section modules and
     * keeps callers from duplicating null-sensitive set membership logic.
     */
    public boolean hasCheck(ExtentSectionCheck check) {
        return checks.contains(Objects.requireNonNull(check, "check"));
    }

    /**
     * Executes the declarative safety rules before a browser is opened. This
     * keeps invalid source/check/journey combinations out of the section
     * modules and makes the annotation an actual plan, not documentation.
     */
    public void validatePlan() {
        if (hasCheck(ExtentSectionCheck.SCREENSHOT_LINK)
                || hasCheck(ExtentSectionCheck.INLINE_ALIGNMENT)) {
            requireSection(ExtentSection.UI_VALIDATION,
                    "screenshot/alignment checks require UI_VALIDATION");
        }
        if (hasCheck(ExtentSectionCheck.JSON_CONTRACT)
                || hasCheck(ExtentSectionCheck.MOCK_RECONCILIATION)) {
            requireAnySection(ExtentSection.TESTS, ExtentSection.EVIDENCE,
                    "JSON/mock checks require TESTS or EVIDENCE");
        }
        if (hasCheck(ExtentSectionCheck.MOCK_RECONCILIATION)
                && source != UiExtentReportSource.GENERATED_API) {
            throw new IllegalArgumentException(
                    "MOCK_RECONCILIATION requires GENERATED_API source");
        }
        if (journey == ExtentJourney.FAILURE_NAVIGATION) {
            requireSection(ExtentSection.FAILS,
                    "FAILURE_NAVIGATION requires FAILS");
            requireSection(ExtentSection.TESTS,
                    "FAILURE_NAVIGATION requires TESTS");
        }
    }

    private void requireSection(ExtentSection section, String message) {
        if (!includes(section)) throw new IllegalArgumentException(message);
    }

    private void requireAnySection(ExtentSection first, ExtentSection second,
                                   String message) {
        if (!includes(first) && !includes(second)) throw new IllegalArgumentException(message);
    }

    private static void validateSource(UiExtentReportSource source,
                                       String reportResource, String reportProperty) {
        switch (source) {
            case GENERATED_API -> {
                if (!reportResource.isEmpty() || !reportProperty.isEmpty()) {
                    throw new IllegalArgumentException(
                            "GENERATED_API cannot also declare reportResource/reportProperty");
                }
            }
            case STATIC_PATH -> {
                if (reportProperty.isEmpty() || !reportResource.isEmpty()) {
                    throw new IllegalArgumentException(
                            "STATIC_PATH requires reportProperty and no reportResource");
                }
            }
            case STATIC_RESOURCE -> {
                if (reportResource.isEmpty() || !reportProperty.isEmpty()) {
                    throw new IllegalArgumentException(
                            "STATIC_RESOURCE requires reportResource and no reportProperty");
                }
            }
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}

