package atf.api.tests.governance.distribution;

import atf.api.tests.governance.AtfGovernanceTestSupport;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
public class ReportingHarnessOwnershipTest {
    @TempDir Path root;
    @Test void liveHarnessHasOneDeclaredOwner() throws Exception {
        Path live = AtfGovernanceTestSupport.locateProjectRoot();
        var ownership = ReportingHarnessOwnership.read(live);
        ownership.inspect(() -> ownership.requireFile(live.resolve("atf-reporting-tests/pom.xml")));
    }
    @Test void declaredSourceAuditsRetainMissingFileAndOriginalFailureControls() throws Exception {
        DeploymentFixture.prepare(root, true);
        var ownership = ReportingHarnessOwnership.read(root);
        Path file = root.resolve("atf-reporting-tests/pom.xml");
        ownership.requireFile(file);
        var original = new AssertionError("original source guard");
        assertSame(original, assertThrows(AssertionError.class, () -> ownership.inspect(() -> { throw original; })));
        Files.delete(file);
        assertThrows(AssertionError.class, () -> ownership.requireFile(file));
    }
    @ParameterizedTest @ValueSource(strings = {"atf-core", "atf-test-auto-ex"})
    void externalOwnershipRejectsCopiedHarnessPackages(String module) throws Exception {
        prepareExternal();
        Path file = root.resolve(module + "/src/test/java/Impostor.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "package atf.reporting.fixture; class Impos" + "tor { }");
        var ownership = ReportingHarnessOwnership.read(root);
        assertThrows(AssertionError.class, () -> ownership.inspect(() -> fail("External consumer cannot run a source-only audit")));
        Files.writeString(file, "package application; class Impos" + "tor { String example = \"package atf.reporting.fixture;\"; }");
        ownership.inspect(() -> fail("The harness is external"));
        ownership.requireFile(root.resolve("atf-reporting-tests/pom.xml"));
    }
    @Test void undeclaredHarnessCannotMasqueradeAsExternalOwnership() throws Exception {
        prepareExternal();Files.createDirectory(root.resolve("atf-reporting-tests"));
        assertThrows(IllegalStateException.class, () -> ReportingHarnessOwnership.read(root));
    }
    private void prepareExternal() throws Exception {
        DeploymentFixture.prepare(root, false);
        Path pom = root.resolve("pom.xml");
        Files.writeString(pom, Files.readString(pom).replace("<module>atf-reporting-tests</module>", ""));
        Files.delete(root.resolve("atf-reporting-tests/pom.xml"));Files.delete(root.resolve("atf-reporting-tests"));
    }
}
