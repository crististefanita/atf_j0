package atf.api.tests.governance.maintenance;

import atf.api.tests.governance.AtfGovernanceTestSupport;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

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

    @Test
    void orchestrationDecisionKeepsLangGraphOptionalAndMavenAuthoritative() throws Exception {
        Path root = AtfGovernanceTestSupport.locateProjectRoot();
        String feedback = Files.readString(root.resolve("docs/feedback/ATF_Maintenance_Feedback.md"));
        String heading = "### Când merită evaluat LangGraph?";
        int start = feedback.indexOf(heading);
        assertTrue(start >= 0, "the maintenance feedback documents the orchestration decision");

        int opening = feedback.indexOf("```mermaid", start);
        int closing = opening < 0 ? -1 : feedback.indexOf("```", opening + "```mermaid".length());
        assertTrue(opening > start && closing > opening, "the orchestration decision has a closed Mermaid diagram");
        String diagram = feedback.substring(opening, closing);
        assertTrue(diagram.contains("flowchart LR"));
        assertTrue(diagram.contains("RuntimeNeed -->|nu| MavenGates"));
        assertTrue(diagram.contains("RuntimeNeed -->|da| LangGraph"));
        assertTrue(diagram.contains("LangGraph -->|lansează porțile| MavenGates"));
        assertTrue(diagram.contains("Maven/JUnit<br/>sursa verdictului"));

        String rationale = feedback.substring(start, opening).toLowerCase(java.util.Locale.ROOT);
        assertTrue(rationale.contains("ramurile finite"));
        assertTrue(rationale.contains("nu justifică un runtime nou"));
        assertTrue(rationale.contains("stare durabilă"));
        assertTrue(rationale.contains("aprobare umană"));
    }
}
