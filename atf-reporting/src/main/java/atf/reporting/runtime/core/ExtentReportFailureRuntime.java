package atf.reporting.runtime.core;

import atf.reporting.runtime.failure.ExtentReportFailureContext;
import atf.reporting.runtime.failure.ExtentReportFailureDetection;
import atf.reporting.runtime.failure.ExtentReportFailureProjection;

/** Runtime facade for the diagnostic Fails projection and deep-links. */
public final class ExtentReportFailureRuntime {
    private ExtentReportFailureRuntime() { }

    /**
     * Composes the failure runtime in semantic order: detection, context,
     * then projection. Each concern remains independently testable.
     */
    public static String script() {
        return String.join("",
                ExtentReportFailureDetection.script(),
                ExtentReportFailureContext.script(),
                ExtentReportFailureProjection.script());
    }

    public static String causeChainScript() {
        return ExtentReportFailureContext.causeChainScript();
    }
}
