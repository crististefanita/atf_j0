package atf.reporting.ui.failure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declarative, plug-and-play composition root for UI failure evidence. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UiFailureReportAudit {
    UiFailureReportInput input() default UiFailureReportInput.GENERATED_FAILURE_FIXTURE;

    String reportProperty() default "atf.ui.failure.report";

    UiFailureSurface[] surfaces() default {
            UiFailureSurface.SETUP,
            UiFailureSurface.PLAYWRIGHT_ACTION,
            UiFailureSurface.AUDIT_OBSERVATION,
            UiFailureSurface.ASSERTION
    };

    String id() default "";
}
