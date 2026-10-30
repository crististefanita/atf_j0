package atf.api.tests.governance.documentation.factual;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static atf.api.tests.governance.AtfGovernanceTestSupport.locateProjectRoot;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
public class DocumentedBuildContractTest {
    @Test void architectureClaimsMatchTheExecutableMavenDeclarations() throws Exception {
        Path root = locateProjectRoot();
        assertEquals(List.of(), DocumentedBuildContracts.inspect(root, architecture(root)));
    }
    @Test void aWrongDocumentedModuleCountFailsWithoutChangingThePom() throws Exception {
        Path root = locateProjectRoot(); byte[] pom = Files.readAllBytes(root.resolve("pom.xml"));
        String source = architecture(root);
        String changed = source.replace("exact cele 12 module existente", "exact cele 13 module existente");
        assertNotEquals(source, changed);
        var failures = DocumentedBuildContracts.inspect(root, changed);
        assertEquals(1, failures.size()); assertTrue(failures.getFirst().contains("declared=13; observed=12; source=pom.xml"));
        assertTrue(failures.getFirst().startsWith("DOC-FACT docs/ATF_Architecture.md:"));
        assertArrayEquals(pom, Files.readAllBytes(root.resolve("pom.xml")));
    }
    @ParameterizedTest @ValueSource(strings={"Shop", "AutoEx"})
    void movingOnlyTheDocumentedKitToCompileIsRejected(String consumer) throws Exception {
        Path root = locateProjectRoot(); String source = architecture(root);
        String row = source.lines().filter(line -> line.startsWith("| " + consumer + " |") && line.split("\\|", -1).length == 5).findFirst().orElseThrow();
        String[] cells = row.split("\\|", -1);
        String changed = source.replace(row, "| " + consumer + " | " + cells[2].strip() + ", Test Kit | — |");
        var failures = DocumentedBuildContracts.inspect(root, changed);
        assertEquals(1, failures.size());
        assertTrue(failures.getFirst().contains(consumer + " Test Kit scope: declared=compile; observed=test; source="));
        assertEquals(List.of(), DocumentedBuildContracts.inspect(root, source));
    }
    @Test void missingAndDuplicatedClaimsCannotSilentlySkipTheContract() throws Exception {
        Path root = locateProjectRoot(); String source = architecture(root);
        String row = source.lines().filter(line -> line.startsWith("| Shop |") && line.split("\\|", -1).length == 5).findFirst().orElseThrow();
        assertFalse(DocumentedBuildContracts.inspect(root, source.replace(row, "")).isEmpty());
        assertFalse(DocumentedBuildContracts.inspect(root, source.replace(row, row + "\n" + row)).isEmpty());
        assertFalse(DocumentedBuildContracts.inspect(root, source.replace("Reactorul păstrează exact cele", "Reactorul are")).isEmpty());
    }
    private static String architecture(Path root) throws Exception { return Files.readString(root.resolve("docs/ATF_Architecture.md")); }
}
