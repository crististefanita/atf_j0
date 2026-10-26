package atf.reporting.ui.contract;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Programmatic composition contract for a static UI Extent report.
 *
 * <p>The input is deliberately a JVM property instead of YAML: a UI report
 * is an artifact under test, not API business configuration. The annotation
 * is renderer-neutral so a consumer can compose a browser audit without
 * moving the contract into the browser test-kit.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UiExtentReportContract {
    String reportProperty() default "atf.ui.report";

    ReportUiContract.Tab[] tabs() default {
            ReportUiContract.Tab.OVERVIEW,
            ReportUiContract.Tab.TESTS,
            ReportUiContract.Tab.CATEGORIES,
            ReportUiContract.Tab.FAILS
    };

    UiReportCheck[] checks() default {
            UiReportCheck.STRUCTURE,
            UiReportCheck.STYLE,
            UiReportCheck.COLLAPSED_BY_DEFAULT,
            UiReportCheck.SETUP_EVIDENCE,
            UiReportCheck.CROSS_TAB,
            UiReportCheck.SCREEN_VALIDATION,
            UiReportCheck.INLINE_SCREENSHOT,
            UiReportCheck.FAILS_PROJECTION
    };

    String id() default "";
}
