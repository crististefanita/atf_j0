package atf.reporting.ui.failure;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Validated interpretation of {@link UiFailureReportAudit}. */
public record UiFailureReportSpec(String id, UiFailureReportInput input,
                                  String reportProperty, Set<UiFailureSurface> surfaces) {
    public UiFailureReportSpec {
        id = required(id, "id");
        input = Objects.requireNonNull(input, "input");
        reportProperty = required(reportProperty, "reportProperty");
        surfaces = Set.copyOf(Objects.requireNonNull(surfaces, "surfaces"));
        if (surfaces.isEmpty()) throw new IllegalArgumentException("surfaces must not be empty");
    }

    public static UiFailureReportSpec resolve(Class<?> owner) {
        Objects.requireNonNull(owner, "owner");
        UiFailureReportAudit annotation = owner.getAnnotation(UiFailureReportAudit.class);
        if (annotation == null) {
            throw new IllegalArgumentException(owner.getName()
                    + " must declare @UiFailureReportAudit");
        }
        String id = annotation.id().isBlank()
                ? owner.getSimpleName().replaceAll("(?<!^)([A-Z])", "-$1")
                    .toLowerCase(Locale.ROOT)
                : annotation.id().trim();
        return new UiFailureReportSpec(id, annotation.input(), annotation.reportProperty(),
                EnumSet.copyOf(Arrays.asList(annotation.surfaces())));
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
