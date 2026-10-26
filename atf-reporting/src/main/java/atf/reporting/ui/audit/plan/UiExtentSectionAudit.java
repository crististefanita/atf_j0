package atf.reporting.ui.audit.plan;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative composition root for one or more independently auditable report
 * sections. The test class declares intent; flows and locators own the browser.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UiExtentSectionAudit {
    UiExtentReportSource source() default UiExtentReportSource.GENERATED_API;

    String reportResource() default "";

    String reportProperty() default "";

    ExtentSection[] sections();

    ExtentSectionCheck[] checks() default {ExtentSectionCheck.STRUCTURE};

    ExtentJourney journey() default ExtentJourney.NONE;

    String id() default "";
}

