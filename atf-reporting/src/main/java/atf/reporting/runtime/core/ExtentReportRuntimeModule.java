package atf.reporting.runtime.core;

import atf.reporting.html.script.HtmlReportScriptBuilder;
import atf.reporting.html.script.HtmlReportScriptModule;
import atf.reporting.runtime.section.ExtentReportRuntimeEvidence;
import atf.reporting.runtime.section.ExtentReportRuntimeNavigation;
import atf.reporting.runtime.section.ExtentReportRuntimeOverview;
import atf.reporting.runtime.section.ExtentReportRuntimeTests;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Typed registry for the browser-runtime module graph.
 *
 * <p>Each semantic runtime controller has exactly one publication owner and one
 * JavaScript producer. Keeping that mapping here makes the composition order
 * explicit and gives component tests one stable seam for checking module
 * cohesion without opening a browser.</p>
 */
public enum ExtentReportRuntimeModule {
    CORE(HtmlReportScriptModule.RUNTIME_CORE, ExtentReportRuntimeCore::script),
    TESTS(HtmlReportScriptModule.RUNTIME_TESTS, ExtentReportRuntimeTests::script),
    OVERVIEW(HtmlReportScriptModule.RUNTIME_OVERVIEW, ExtentReportRuntimeOverview::script),
    EVIDENCE(HtmlReportScriptModule.RUNTIME_EVIDENCE, ExtentReportRuntimeEvidence::script),
    NAVIGATION(HtmlReportScriptModule.RUNTIME_NAVIGATION, ExtentReportRuntimeNavigation::script),
    DURATION(HtmlReportScriptModule.RUNTIME_DURATION, ExtentReportDurationRuntime::script),
    FAILURES(HtmlReportScriptModule.RUNTIME_FAILURES, ExtentReportFailureRuntime::script),
    FAILURE_CAUSE(HtmlReportScriptModule.RUNTIME_FAILURE_CAUSE, ExtentReportFailureRuntime::causeChainScript);

    private final HtmlReportScriptModule publicationOwner;
    private final Supplier<String> source;

    ExtentReportRuntimeModule(HtmlReportScriptModule publicationOwner, Supplier<String> source) {
        this.publicationOwner = Objects.requireNonNull(publicationOwner, "publicationOwner");
        this.source = Objects.requireNonNull(source, "source");
    }

    public HtmlReportScriptModule publicationOwner() {
        return publicationOwner;
    }

    public String script() {
        return Objects.requireNonNull(source.get(), name() + " script");
    }

    public void appendTo(HtmlReportScriptBuilder builder) {
        Objects.requireNonNull(builder, "builder").append(publicationOwner, script());
    }
}
