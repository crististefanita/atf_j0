package atf.reporting.runtime.section;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Runtime facade for the Overview section and its typed JavaScript fragments. */
public final class ExtentReportRuntimeOverview {
    private static final List<Fragment> FRAGMENTS = List.of(Fragment.values());

    private ExtentReportRuntimeOverview() { }

    public static String script() {
        return String.join("", FRAGMENTS.stream().map(Fragment::script).toList());
    }

    static List<Fragment> fragments() {
        return FRAGMENTS;
    }

    enum Fragment {
        AUDIT(ExtentReportRuntimeOverviewAudit::script),
        LABELS(ExtentReportRuntimeOverviewLabels::script),
        CHARTS(ExtentReportRuntimeOverviewCharts::script),
        CLEANUP(ExtentReportRuntimeOverviewCleanup::script);

        private final Supplier<String> source;

        Fragment(Supplier<String> source) {
            this.source = Objects.requireNonNull(source, "source");
        }

        String script() {
            return Objects.requireNonNull(source.get(), name() + " script");
        }
    }
}
