package atf.autoex.tests.extension;

import atf.console.junit.AtfConsoleTestExtension;
import atf.testkit.AtfConsoleMessages;
import atf.testkit.AtfE2eTest;
import atf.testkit.output.UiReportSubdirectory;
import atf.testkit.reporting.AtfReportPath;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Shared AutoEx E2E infrastructure; channel and scenario-specific evidence stay on each test. */
@AtfE2eTest
@UiReportSubdirectory("auto-ex")
@AtfConsoleMessages(AtfConsoleMessages.Policy.DETAILS)
@AtfReportPath
@ExtendWith({AtfConsoleTestExtension.class, AutoExE2eExtension.class})
@Execution(ExecutionMode.SAME_THREAD)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoExE2eTest { }
