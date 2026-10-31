package atf.api.tests.governance.documentation.factual;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
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
        var declaration = Pattern.compile("exact cele (\\d+) module existente").matcher(source);
        assertTrue(declaration.find(), "The source must expose one module declaration");
        int observed = Integer.parseInt(declaration.group(1));
        String changed = source.substring(0, declaration.start(1)) + (observed + 1) + source.substring(declaration.end(1));
        assertNotEquals(source, changed);
        var failures = DocumentedBuildContracts.inspect(root, changed);
        assertEquals(1, failures.size()); assertTrue(failures.getFirst().contains("declared=" + (observed + 1) + "; observed=" + observed + "; source=pom.xml"));
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
        String observed = Files.isRegularFile(root.resolve(consumer.equals("Shop") ? "atf-test-practice-shop/pom.xml" : "atf-test-auto-ex/pom.xml")) ? "test" : "absent";
        assertTrue(failures.getFirst().contains(consumer + " Test Kit scope: declared=compile; observed=" + observed + "; source="));
        assertEquals(List.of(), DocumentedBuildContracts.inspect(root, source));
    }
    @Test void missingAndDuplicatedClaimsCannotSilentlySkipTheContract() throws Exception {
        Path root = locateProjectRoot(); String source = architecture(root);
        String row = source.lines().filter(line -> line.startsWith("| Shop |") && line.split("\\|", -1).length == 5).findFirst().orElseThrow();
        assertFalse(DocumentedBuildContracts.inspect(root, source.replace(row, "")).isEmpty());
        assertFalse(DocumentedBuildContracts.inspect(root, source.replace(row, row + "\n" + row)).isEmpty());
        assertFalse(DocumentedBuildContracts.inspect(root, source.replace("Reactorul păstrează exact cele", "Reactorul are")).isEmpty());
    }
    @ParameterizedTest @ValueSource(ints={8, 9, 10, 11, 12})
    void moduleCountMutantIsIndependentOfTheDeploymentSize(int count, @TempDir Path root) throws Exception {
        String modules = java.util.stream.IntStream.range(0, count).mapToObj(n -> "<module>" + (n == 0 ? "atf-test-practice-shop" : n == 1 ? "atf-test-auto-ex" : "module-" + n) + "</module>").collect(java.util.stream.Collectors.joining());
        Files.writeString(root.resolve("pom.xml"), "<project><modules>" + modules + "</modules></project>");
        for (String module : List.of("atf-test-practice-shop", "atf-test-auto-ex")) {
            Files.createDirectory(root.resolve(module));
            Files.writeString(root.resolve(module).resolve("pom.xml"), "<project><dependencies><dependency><artifactId>atf-test-kit</artifactId><scope>test</scope></dependency></dependencies></project>");
        }
        String document = "Reactorul păstrează exact cele " + count + " module existente.\n"
                + "| Consumer | `compile` | `test` |\n|---|---|---|\n| Shop | Core | Test Kit |\n| AutoEx | Core | Test Kit |\n";
        assertEquals(List.of(), DocumentedBuildContracts.inspect(root, document));
        var failures = DocumentedBuildContracts.inspect(root, document.replace("cele " + count, "cele " + (count + 1)));
        assertEquals(1, failures.size());
        assertTrue(failures.getFirst().contains("declared=" + (count + 1) + "; observed=" + count + "; source=pom.xml"));
    }
    private static String architecture(Path root) throws Exception { return Files.readString(root.resolve("docs/ATF_Architecture.md")); }
}
