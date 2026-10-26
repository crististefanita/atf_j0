package atf.reporting.runtime.core;
import atf.reporting.asset.ReportAssetComposer;

import java.util.stream.Collectors;

/**
 * Composes the semantic browser-runtime controllers injected into the vendor document.
 *
 * Publication remains responsible for the file; each controller owns one runtime
 * boundary so navigation, Tests, evidence and Overview can be reviewed independently.
 */
public final class ExtentReportRuntimeBehaviour {
    private static final String HEAD_CLOSE = "</head>";
    private static final String SCRIPT_OPEN_PREFIX = "<script id=\"";
    private static final String SCRIPT_OPEN_SUFFIX = "\" type=\"text/javascript\">";
    private static final String SCRIPT_CLOSE = "</script>";
    private static final String RUNTIME_ELEMENT_ID = "atf-report-behaviour";
    private ExtentReportRuntimeBehaviour() { }

    public static String inject(String html) {
        String javascript = runtimeScript();
        return html.replace(HEAD_CLOSE, SCRIPT_OPEN_PREFIX + RUNTIME_ELEMENT_ID
                + SCRIPT_OPEN_SUFFIX + javascript + SCRIPT_CLOSE + HEAD_CLOSE);
    }

    private static String runtimeScript() {
        String modules = java.util.Arrays.stream(ExtentReportRuntimeModule.values())
                .map(ExtentReportRuntimeModule::script)
                .collect(Collectors.joining());
        return ReportAssetComposer.runtimeBootScript(modules);
    }
}
