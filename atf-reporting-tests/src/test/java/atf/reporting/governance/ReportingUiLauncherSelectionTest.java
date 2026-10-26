package atf.reporting.governance;

import atf.testkit.AtfComponentTest;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.ui.tests.extent.e2e.ExtentReportingE2eSuite;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/** Discovery-only proof that the compatibility launcher adds no second browser inventory. */
@AtfComponentTest
class ReportingUiLauncherSelectionTest {
    @Test
    void canonicalAndAliasSelectTheSameMethodAndTemplateIdentities() {
        List<String> canonical = methodIdentities(ExtentReportingE2eSuite.class);
        List<String> alias = methodIdentities(AtfReportingUiAuditSuite.class);

        assertFalse(canonical.isEmpty(), "UI E2E discovery must select executable methods");
        assertEquals(canonical, alias, "alias must not add or remove methods or parameterized templates");
        assertEquals(canonical.size(), canonical.stream().distinct().count(),
                "one launcher must select each method/template only once");
        AtfTestChannel channel = ExtentReportingE2eSuite.class.getAnnotation(AtfTestChannel.class);
        assertNotNull(channel, "canonical launcher must retain its UI metadata");
        assertEquals(TestChannel.UI, channel.value());
        assertTrue(AtfReportingUiAuditSuite.class.isAnnotationPresent(Deprecated.class));
    }

    private static List<String> methodIdentities(Class<?> suite) {
        var request = LauncherDiscoveryRequestBuilder.request().selectors(selectClass(suite)).build();
        TestPlan plan = LauncherFactory.create().discover(request);
        return plan.getRoots().stream()
                .flatMap(root -> plan.getDescendants(root).stream())
                .map(identifier -> identifier.getSource().orElse(null))
                .filter(MethodSource.class::isInstance)
                .map(MethodSource.class::cast)
                .map(source -> source.getClassName() + "#" + source.getMethodName()
                        + "(" + source.getMethodParameterTypes() + ")")
                .sorted()
                .toList();
    }
}
