package atf.reporting.runtime.core;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Facade composing duration parsing, aggregation, projection and lifecycle fragments. */
public final class ExtentReportDurationRuntime {
    private ExtentReportDurationRuntime() { }

    public static String script() {
        return Arrays.stream(Fragment.values())
                .map(Fragment::source)
                .collect(Collectors.joining());
    }

    enum Fragment {
        PARSING(ExtentReportDurationParsing.script()),
        DOM(ExtentReportDurationDom.script()),
        MEMORY(ExtentReportDurationMemory.script()),
        AGGREGATION(ExtentReportDurationAggregation.script()),
        PROJECTION(ExtentReportDurationProjection.script()),
        SCHEDULING(ExtentReportDurationScheduling.script()),
        LIFECYCLE(ExtentReportDurationLifecycle.script());

        private final String source;

        Fragment(String source) {
            this.source = source;
        }

        String source() {
            return source;
        }
    }
}
