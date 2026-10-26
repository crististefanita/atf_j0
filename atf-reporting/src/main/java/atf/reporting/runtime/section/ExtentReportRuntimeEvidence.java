package atf.reporting.runtime.section;

import atf.reporting.runtime.evidence.ExtentReportRuntimeEvidenceDisclosure;
import atf.reporting.runtime.evidence.ExtentReportRuntimeEvidenceFailures;
import atf.reporting.runtime.evidence.ExtentReportRuntimeEvidenceLifecycle;
import atf.reporting.runtime.evidence.ExtentReportRuntimeEvidenceRoles;
import atf.reporting.runtime.evidence.ExtentReportRuntimeEvidenceTableLayout;

/** Public facade for semantic evidence roles, disclosures and UI failure projection. */
public final class ExtentReportRuntimeEvidence {
    private ExtentReportRuntimeEvidence() {}

    public enum Fragment {
        ROLES(ExtentReportRuntimeEvidenceRoles::script),
        DISCLOSURE(ExtentReportRuntimeEvidenceDisclosure::script),
        FAILURES(ExtentReportRuntimeEvidenceFailures::script),
        TABLE_LAYOUT(ExtentReportRuntimeEvidenceTableLayout::script),
        LIFECYCLE(ExtentReportRuntimeEvidenceLifecycle::script);

        private final java.util.function.Supplier<String> source;

        Fragment(java.util.function.Supplier<String> source) {
            this.source = source;
        }

        String source() {
            return source.get();
        }
    }

    public static String script() {
        return java.util.Arrays.stream(Fragment.values())
                .map(Fragment::source)
                .collect(java.util.stream.Collectors.joining());
    }
}

