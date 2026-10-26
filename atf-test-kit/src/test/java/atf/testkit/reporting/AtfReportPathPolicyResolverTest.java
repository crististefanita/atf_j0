package atf.testkit.reporting;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest
class AtfReportPathPolicyResolverTest {
    @Test
    void absenceDisablesAndDirectDeclarationOptsIn() {
        assertEquals(AtfReportPath.Policy.DISABLED,
                AtfReportPathPolicyResolver.resolve(Unconfigured.class));
        assertEquals(AtfReportPath.Policy.ENABLED,
                AtfReportPathPolicyResolver.resolve(DirectOptIn.class));
    }

    @Test
    void policySupportsMetaAnnotationsAndNearestSuperclassOverride() {
        assertEquals(AtfReportPath.Policy.ENABLED,
                AtfReportPathPolicyResolver.resolve(MetaOptIn.class));
        assertEquals(AtfReportPath.Policy.DISABLED,
                AtfReportPathPolicyResolver.resolve(ExplicitOptOut.class));
    }

    @Test
    void conflictingComposedPoliciesFailBeforeReportSetup() {
        assertThrows(IllegalStateException.class,
                () -> AtfReportPathPolicyResolver.resolve(Conflicting.class));
    }

    static class Unconfigured { }

    @AtfReportPath
    static class DirectOptIn { }

    @TestReportPathOwner
    static class MetaOptIn { }

    @AtfReportPath
    static class InheritedOwner { }

    @AtfReportPath(AtfReportPath.Policy.DISABLED)
    static class ExplicitOptOut extends InheritedOwner { }

    @AtfReportPath
    @TestDisabledReportPathOwner
    static class Conflicting { }
}
