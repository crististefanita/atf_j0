package atf.reporting.runtime.section;

/** Facade for tab navigation, deep-links, DOM contracts and disclosure interaction. */
public final class ExtentReportRuntimeNavigation {
    private ExtentReportRuntimeNavigation() { }

    public static String script() {
        return String.join("", java.util.Arrays.stream(Fragment.values()).map(Fragment::source).toList());
    }

    private enum Fragment {
        DOM_CONTRACT(ExtentReportRuntimeNavigationDom.script()),
        DISCLOSURE(ExtentReportRuntimeNavigationDisclosure.script()),
        PERFORMANCE(ExtentReportRuntimeNavigationPerformance.script()),
        CONTROLLER(ExtentReportRuntimeNavigationController.script());

        private final String source;

        Fragment(String source) {
            this.source = source;
        }

        String source() {
            return source;
        }
    }
}
