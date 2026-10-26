package atf.reporting.ui.contract;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/** Immutable, validated interpretation of {@link UiExtentReportContract}. */
public record UiReportContractSpec(String id, String reportProperty,
                                   Set<ReportUiContract.Tab> tabs,
                                   Set<UiReportCheck> checks)
        implements UiReportContractSpecView {
    public UiReportContractSpec {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
        id = id.trim();
        if (reportProperty == null || reportProperty.isBlank()) {
            throw new IllegalArgumentException("reportProperty must not be blank");
        }
        reportProperty = reportProperty.trim();
        tabs = Set.copyOf(tabs == null ? Set.of() : tabs);
        checks = Set.copyOf(checks == null ? Set.of() : checks);
        if (tabs.isEmpty()) throw new IllegalArgumentException("tabs must not be empty");
        if (checks.isEmpty()) throw new IllegalArgumentException("checks must not be empty");
        if (checks.contains(UiReportCheck.UI_VALIDATION)
                || checks.contains(UiReportCheck.INLINE_SCREENSHOT)) {
            if (!tabs.contains(ReportUiContract.Tab.TESTS)) {
                throw new IllegalArgumentException(
                        "UI validation checks require the TESTS tab");
            }
        }
        if (checks.contains(UiReportCheck.FAILS_PROJECTION)
                && !tabs.contains(ReportUiContract.Tab.FAILS)) {
            throw new IllegalArgumentException("FAILS_PROJECTION requires the FAILS tab");
        }
    }

    public static UiReportContractSpec resolve(Class<?> owner) {
        if (owner == null) throw new NullPointerException("owner");
        UiExtentReportContract annotation = owner.getAnnotation(UiExtentReportContract.class);
        if (annotation == null) {
            throw new IllegalArgumentException(owner.getName()
                    + " must declare @UiExtentReportContract");
        }
        String id = annotation.id().isBlank()
                ? owner.getSimpleName().replaceAll("(?<!^)([A-Z])", "-$1")
                    .toLowerCase(Locale.ROOT)
                : annotation.id().trim();
        return new UiReportContractSpec(id, annotation.reportProperty(),
                EnumSet.copyOf(Arrays.asList(annotation.tabs())),
                EnumSet.copyOf(Arrays.asList(annotation.checks())));
    }

    @Override
    public boolean has(UiReportCheck check) {
        return checks.contains(check);
    }
}
