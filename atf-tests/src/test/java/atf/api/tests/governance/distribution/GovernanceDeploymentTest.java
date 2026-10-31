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
        assertEquals(List.of("docs/ATF_Reporting_Library_Atlas.md", "docs/ATF_Reporting_Consumer.md"),
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

    @Test void foundationRequiresItsExplicitProfileAndKeepsEightOrNineModules() throws Exception {
        foundation(false);
        assertEquals(8, GovernanceDeployment.read(root).sourceModules().size());
        Path pom = root.resolve("pom.xml"); String original = Files.readString(pom);
        Files.writeString(pom, original.replace("<atf.consumer.profile>foundation</atf.consumer.profile>", ""));
        assertTrue(assertThrows(IllegalStateException.class, () -> GovernanceDeployment.read(root)).getMessage().startsWith("GOV-REACTOR:"));
        Files.writeString(pom, original.replace("</modules>", "<module>atf-reporting-tests</module></modules>"));
        Files.createDirectory(root.resolve("atf-reporting-tests")); Files.writeString(root.resolve("atf-reporting-tests/pom.xml"), "<project/>");
        assertEquals(9, GovernanceDeployment.read(root).sourceModules().size());
    }

    @Test void foundationCannotLoseACommonModuleOrRetainAnApplicationDirectory() throws Exception {
        foundation(false);
        Path pom = root.resolve("pom.xml"); String original = Files.readString(pom);
        Files.writeString(pom, original.replace("<module>atf-ui</module>", ""));
        assertTrue(assertThrows(IllegalStateException.class, () -> GovernanceDeployment.read(root)).getMessage().startsWith("GOV-REACTOR:"));
        Files.writeString(pom, original); Files.createDirectory(root.resolve("atf-test-auto-ex"));
        assertEquals("GOV-CONSUMER-SHADOW: atf-test-auto-ex", assertThrows(IllegalStateException.class, () -> GovernanceDeployment.read(root)).getMessage());
    }

    @Test void unknownProfilesAndSourceProfileOverridesAreRejected() throws Exception {
        foundation(false); Path pom = root.resolve("pom.xml");
        Files.writeString(pom, Files.readString(pom).replace(">foundation<", ">foundaton<"));
        assertEquals("GOV-CONSUMER-PROFILE: foundaton", assertThrows(IllegalStateException.class, () -> GovernanceDeployment.read(root)).getMessage());
        prepare(true); Files.writeString(pom, Files.readString(pom).replace("<properties>", "<properties><atf.consumer.profile>foundation</atf.consumer.profile>"));
        assertEquals("GOV-CONSUMER-PROFILE: foundation", assertThrows(IllegalStateException.class, () -> GovernanceDeployment.read(root)).getMessage());
    }

    @Test void foundationInspectsRealNamespacesAndDoesNotConfuseExamplesWithCode() throws Exception {
        foundation(false); Path source = root.resolve("atf-core/src/test/java/fixture/Example.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "package fixture; class Example { String sample = \"import atf.shop.Order;\"; }");
        assertEquals(8, GovernanceDeployment.read(root).sourceModules().size());
        Files.writeString(source, "package fixture; import atf.shop.Order; class Example { }");
        assertEquals("GOV-CONSUMER-NAMESPACE: atf-core", assertThrows(IllegalStateException.class, () -> GovernanceDeployment.read(root)).getMessage());
    }

    @Test void foundationDocumentationTargetsItsOwnContractAndPreservesUnknownTargetRefusal() throws Exception {
        foundation(false); var deployment = GovernanceDeployment.read(root);
        assertEquals(List.of("docs/ATF_Reporting_Consumer.md", "docs/ATF_Workspace_Synchronization.md"),
                deployment.documentationTargets("atf-test-practice-shop/README.md"));
        assertEquals(List.of("missing.md"), deployment.documentationTargets("missing.md"));
    }

    private void foundation(boolean harness) throws Exception {
        prepare(false); Path pom = root.resolve("pom.xml"); String xml = Files.readString(pom);
        for (String module : List.of("atf-test-practice-shop", "atf-test-auto-ex", "atf-reporting-tests")) {
            if (harness && module.equals("atf-reporting-tests")) continue;
            xml = xml.replace("<module>" + module + "</module>", "");
            Files.delete(root.resolve(module + "/pom.xml")); Files.delete(root.resolve(module));
        }
        Files.writeString(pom, xml.replace("<properties>", "<properties><atf.consumer.profile>foundation</atf.consumer.profile>"));
    }
}
