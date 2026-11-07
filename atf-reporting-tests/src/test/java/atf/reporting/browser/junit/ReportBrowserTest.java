package atf.reporting.browser.junit;

import java.lang.annotation.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

/** Executes the unchanged JUnit invocation in an exclusively owned, phase-supervised worker. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Tag("report-browser")
@ExtendWith(BrowserTestBoundary.class)
public @interface ReportBrowserTest { }
