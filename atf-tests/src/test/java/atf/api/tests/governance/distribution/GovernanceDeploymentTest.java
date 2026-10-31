package atf.api.tests.governance.distribution;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
public class GovernanceDeploymentTest {
    @TempDir Path root;

    @Test void sourceAndBinaryDeploymentsKeepRealDocumentationTargets() throws Exception {
        prepare(true);
        assertEquals(List.of("atf-reporting/README.md"), GovernanceDeployment.read(root).documentationTargets("atf-reporting/README.md"));
        prepare(false);
        assertEquals(List.of("docs/ATF_Reporting_Library_Atlas.md", "docs/ATF_J1_Consumer.md", "lib/README.md"),
                GovernanceDeployment.read(root).documentationTargets("atf-reporting/README.md"));
        assertEquals(List.of("missing.md"), GovernanceDeployment.read(root).documentationTargets("missing.md"),
                "An arbitrary absent target must never be silently substituted");
    }
    @Test void aDeletedModuleOrPomCannotChooseASmallerPerimeter() throws Exception {
        prepare(false);
        var pom = root.resolve("pom.xml");
        String original = Files.readString(pom);
        Files.writeString(pom, original.replace("<module>atf-ui</module>", ""));
        assertTrue(assertThrows(IllegalStateException.class, () -> GovernanceDeployment.read(root)).getMessage().startsWith("GOV-REACTOR:"));
        Files.writeString(pom, original);
        Files.delete(root.resolve("atf-ui/pom.xml"));
        assertEquals("GOV-MODULE-POM: atf-ui", assertThrows(IllegalStateException.class, () -> GovernanceDeployment.read(root)).getMessage());
    }
    @Test void binaryModeRequiresAnExplicitCoordinateAndRejectsShadowSource() throws Exception {
        prepare(false);
        var pom = root.resolve("pom.xml"); String original = Files.readString(pom);
        Files.writeString(pom, original.replace("<atf.reporting.version>fixture</atf.reporting.version>", ""));
        assertTrue(assertThrows(IllegalStateException.class, () -> GovernanceDeployment.read(root)).getMessage().startsWith("GOV-BINARY-VERSION:"));
        Files.writeString(pom, original); Files.createDirectory(root.resolve("atf-reporting"));
        assertTrue(assertThrows(IllegalStateException.class, () -> GovernanceDeployment.read(root)).getMessage().startsWith("GOV-SHADOW:"));
    }
    @Test void duplicateAndUnknownModulesCannotBroadenTheContract() throws Exception {
        prepare(true); var pom = root.resolve("pom.xml"); String original = Files.readString(pom);
        for (String addition : List.of("atf-core", "atf-unknown")) {
            Files.writeString(pom, original.replace("</modules>", "<module>" + addition + "</module></modules>"));
            assertTrue(assertThrows(IllegalStateException.class, () -> GovernanceDeployment.read(root)).getMessage().startsWith("GOV-REACTOR:"));
        }
    }
    private void prepare(boolean reportingSource) throws Exception { DeploymentFixture.prepare(root, reportingSource); }

    @Test void binaryConsumerAndExternalHarnessKeepExplicitCatalogs() throws Exception {
        prepare(false);
        assertEquals(11, GovernanceDeployment.read(root).sourceModules().size());
        var pom = root.resolve("pom.xml");
        Files.writeString(pom, Files.readString(pom).replace("<module>atf-reporting-tests</module>", ""));
        assertTrue(assertThrows(IllegalStateException.class, () -> GovernanceDeployment.read(root))
                .getMessage().startsWith("GOV-SHADOW:"));
        Files.delete(root.resolve("atf-reporting-tests/pom.xml"));
        Files.delete(root.resolve("atf-reporting-tests"));
        assertEquals(List.of("atf-test-kit", "atf-core", "atf-mock", "atf-validation", "atf-console", "atf-ui",
                        "atf-debug-ui", "atf-test-practice-shop", "atf-test-auto-ex", "atf-tests"),
                GovernanceDeployment.read(root).sourceModules());
        Files.writeString(pom, Files.readString(pom).replace("<module>atf-core</module>", ""));
        assertTrue(assertThrows(IllegalStateException.class, () -> GovernanceDeployment.read(root))
                .getMessage().startsWith("GOV-REACTOR:"));
    }

    @Test void sourceCannotRetireItsCanonicalHarness() throws Exception {
        prepare(true);
        var pom = root.resolve("pom.xml");
        Files.writeString(pom, Files.readString(pom).replace("<module>atf-reporting-tests</module>", ""));
        assertTrue(assertThrows(IllegalStateException.class, () -> GovernanceDeployment.read(root))
                .getMessage().startsWith("GOV-REACTOR:"));
    }
}
