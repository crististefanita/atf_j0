package atf.core.testing.api;

import atf.core.reporting.contract.AtfReportStatus;

import java.util.List;
import java.util.Objects;

/** Immutable identity of one API scenario selected by an annotated audit. */
public record ApiScenarioSelection(Class<?> ownerType, String method, String family,
                                   String reportOwner, String scenarioTitle,
                                   List<String> reportScenarioTitles,
                                   List<AtfReportStatus> reportScenarioStatuses) {
    public ApiScenarioSelection(Class<?> ownerType, String method, String family,
                                String reportOwner, String scenarioTitle) {
        this(ownerType, method, family, reportOwner, scenarioTitle, List.of(scenarioTitle),
                List.of(AtfReportStatus.PASS));
    }

    public ApiScenarioSelection(Class<?> ownerType, String method, String family,
                                String reportOwner, String scenarioTitle,
                                List<String> reportScenarioTitles) {
        this(ownerType, method, family, reportOwner, scenarioTitle, reportScenarioTitles,
                defaultStatuses(reportScenarioTitles));
    }

    public ApiScenarioSelection {
        ownerType = Objects.requireNonNull(ownerType, "ownerType");
        method = required(method, "method");
        family = required(family, "family");
        reportOwner = required(reportOwner, "reportOwner");
        scenarioTitle = required(scenarioTitle, "scenarioTitle");
        reportScenarioTitles = List.copyOf(Objects.requireNonNull(reportScenarioTitles,
                "reportScenarioTitles"));
        reportScenarioStatuses = List.copyOf(Objects.requireNonNull(reportScenarioStatuses,
                "reportScenarioStatuses"));
        if (reportScenarioTitles.isEmpty()
                || reportScenarioTitles.stream().anyMatch(title -> title == null || title.isBlank())) {
            throw new IllegalArgumentException("reportScenarioTitles must contain at least one non-blank title");
        }
        if (reportScenarioStatuses.size() != reportScenarioTitles.size()
                || reportScenarioStatuses.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "reportScenarioStatuses must align one-to-one with reportScenarioTitles");
        }
    }

    public String reference() {
        return ownerType.getSimpleName() + "." + method;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static List<AtfReportStatus> defaultStatuses(List<String> titles) {
        Objects.requireNonNull(titles, "reportScenarioTitles");
        return java.util.Collections.nCopies(titles.size(), AtfReportStatus.PASS);
    }
}
