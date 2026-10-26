package atf.ui.tests.extent.component.architecture;
import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.api.ApiReportAuditSetup;
import atf.ui.tests.extent.support.config.ApiReportEnvironmentSnapshotFactory;
import atf.ui.tests.extent.support.data.UiTestData;

import atf.ui.config.UiBrowserConfiguration;
import atf.ui.tests.extent.support.config.UiConfiguration;
import atf.ui.config.UiScreenValidationConfiguration;
import atf.ui.tests.extent.support.api.ApiReportSetupDeclarations;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@AtfComponentTest
class ApiReportAuditSetupTest {
    @Test
    void annotationOverridesTheBroaderYamlCollectionForOneUiArtifact() {
        UiConfiguration yamlFallback = UiConfiguration.defaults(Path.of("target/api-setup-test"));

        assertEquals(List.of("users", "posts"),
                ApiReportAuditSetup.families(ApiReportSetupDeclarations.UsersPosts.class, yamlFallback));
    }

    @Test
    void emptyAnnotationDelegatesToTheYamlCollection() {
        UiConfiguration yamlFallback = new UiConfiguration(
                UiBrowserConfiguration.defaults(), Path.of("target/api-setup-test"), "ui_",
                List.of("posts", "users"), UiScreenValidationConfiguration.defaults(), Optional.empty());

        assertEquals(List.of("posts", "users"),
                ApiReportAuditSetup.families(ApiReportSetupDeclarations.Empty.class, yamlFallback));
    }

    @Test
    void manualJvmCollectionOverridesAnnotationForAdHocReportValidation() {
        String previous = System.getProperty(UiTestData.API_FAMILIES_PROPERTY);
        try {
            System.setProperty(UiTestData.API_FAMILIES_PROPERTY,
                    UiTestData.REQUESTS_FAMILY + "," + UiTestData.HEADERS_FAMILY);
            assertEquals(List.of(UiTestData.REQUESTS_FAMILY, UiTestData.HEADERS_FAMILY),
                    ApiReportAuditSetup.families(ApiReportSetupDeclarations.UsersPosts.class,
                            UiConfiguration.defaults(Path.of("target/api-setup-test"))));
        } finally {
            if (previous == null) System.clearProperty(UiTestData.API_FAMILIES_PROPERTY);
            else System.setProperty(UiTestData.API_FAMILIES_PROPERTY, previous);
        }
    }

    @Test
    void manualJvmFamilyOverrideTakesPrecedenceOverExactTestDeclaration() {
        var selection = ApiReportAuditSetup.selection(
                ApiReportSetupDeclarations.ExactPostCreate.class,
                UiConfiguration.defaults(Path.of("target/api-setup-test")),
                UiTestData.REQUESTS_FAMILY + "," + UiTestData.HEADERS_FAMILY, null);

        assertEquals(List.of(UiTestData.REQUESTS_FAMILY, UiTestData.HEADERS_FAMILY),
                selection.families());
        assertEquals(List.of(), selection.selectedTests());
    }

    @Test
    void apiPreconditionMetadataIsTypedAndBoundedAtItsCompositionBoundary() {
        var snapshot = ApiReportEnvironmentSnapshotFactory.precondition(
                ApiReportSetupDeclarations.UsersPosts.class, List.of("users", "posts"));

        assertEquals("ACCEPTANCE", snapshot.displayValues().get("Environment"));
        assertEquals("users, posts", snapshot.displayValues().get("Profile"));
        assertEquals("atf-mock.yaml", snapshot.displayValues().get("Precondition"));
        assertEquals("Extent HTML", snapshot.displayValues().get("Reporting"));
    }
}
