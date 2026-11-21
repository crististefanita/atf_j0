package atf.api.tests.governance.maintenance;

import atf.api.tests.governance.AtfGovernanceTestSupport;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps the visible maintenance flow and its executable pair gate connected. */
@AtfComponentTest
public class AtfMaintenanceProcessDocumentationGovernanceTest {
    @Test
    void workspaceProtocolConnectsFeedbackGatesConsumerAcceptanceAndPromotion() throws Exception {
        var root = AtfGovernanceTestSupport.locateProjectRoot();
        String synchronization = Files.readString(root.resolve("docs/ATF_Workspace_Synchronization.md"));
        int protocolStart = synchronization.indexOf("## Protocolul fiecărui increment");
        int protocolEnd = synchronization.indexOf("## Proveniență și relocare", protocolStart);
        assertTrue(protocolStart >= 0 && protocolEnd > protocolStart, "the maintenance protocol section has stable boundaries");
        String protocol = synchronization.substring(protocolStart, protocolEnd);
        assertTrue(protocol.contains("flowchart LR"));
        assertTrue(protocol.contains("Feedback"));
        assertTrue(protocol.contains("Teste native"));
        assertTrue(protocol.contains("Whole J0"));
        assertTrue(protocol.contains("Acceptanță pereche"));
        assertTrue(protocol.contains("J1"));
        assertTrue(protocol.contains("JAR + POM"));
        assertTrue(protocol.contains("Origin"));
        assertTrue(protocol.contains("-.->|gap|"));

        String governance = Files.readString(root.resolve("atf-tests/README.md"));
        assertTrue(governance.contains("AtfWorkspaceMaintenanceSuite"));
        assertTrue(governance.contains("AtfMaintenanceWorkspacePairSuite"));
        assertTrue(governance.contains("WholeAtfGovernanceSuiteTest"));
        assertTrue(governance.contains("| Poartă |"));
        assertTrue(governance.contains("-Datf.maintenance.j0="));
        assertTrue(governance.contains("-Datf.maintenance.j1="));
        assertTrue(governance.contains("read-only"));

        String suite = Files.readString(root.resolve(
                "atf-tests/src/test/java/atf/api/tests/governance/maintenance/AtfWorkspaceMaintenanceSuite.java"));
        assertTrue(suite.contains("WholeAtfGovernanceSuiteTest.class"));
        assertTrue(suite.contains("AtfMaintenanceWorkspacePairGovernanceTest.class"));
    }
}
