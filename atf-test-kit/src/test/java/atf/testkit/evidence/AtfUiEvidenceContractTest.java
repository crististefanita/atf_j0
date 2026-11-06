package atf.testkit.evidence;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Locks the shared declaration surface without pulling in a reporting implementation. */
@AtfComponentTest
class AtfUiEvidenceContractTest {
    @AtfUiEvidence
    private static class Defaults { }

    @AtfUiEvidence(flow = AtfUiEvidenceFlow.UNIFIED_FLOW,
            screenshots = AtfUiScreenshotPolicy.FINAL_ONLY)
    private static class FinalOnly { }

    @Test
    void declarationSeparatesFlowAndScreenshotAxesWithConfigurationDefaults() {
        var defaults = Defaults.class.getAnnotation(AtfUiEvidence.class);
        var finalOnly = FinalOnly.class.getAnnotation(AtfUiEvidence.class);
        AuditedAssertions.assertAll(
                () -> AuditedAssertions.assertEquals(AtfUiEvidenceFlow.CONFIGURED, defaults.flow()),
                () -> AuditedAssertions.assertEquals(AtfUiScreenshotPolicy.CONFIGURED, defaults.screenshots()),
                () -> AuditedAssertions.assertEquals(AtfUiEvidenceFlow.UNIFIED_FLOW, finalOnly.flow()),
                () -> AuditedAssertions.assertEquals(AtfUiScreenshotPolicy.FINAL_ONLY, finalOnly.screenshots()),
                () -> AuditedAssertions.assertEquals(RetentionPolicy.RUNTIME,
                        AtfUiEvidence.class.getAnnotation(Retention.class).value()),
                () -> AuditedAssertions.assertEquals(ElementType.TYPE,
                        AtfUiEvidence.class.getAnnotation(Target.class).value()[0]));
    }
}
