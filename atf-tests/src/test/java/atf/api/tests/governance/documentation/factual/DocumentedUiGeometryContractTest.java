package atf.api.tests.governance.documentation.factual;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static atf.api.tests.governance.AtfGovernanceTestSupport.locateProjectRoot;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
public class DocumentedUiGeometryContractTest {
    @Test void selectedClaimsMatchLiveYamlAndTheExecutableUiGeometry() throws Exception {
        Path root = locateProjectRoot();
        for (String document : DocumentedUiGeometryContracts.DOCUMENTS)
            assertEquals(List.of(), inspect(root, document, read(root, document)), document);
    }

    @ParameterizedTest @ValueSource(strings = {"atf-test-auto-ex/README.md", "atf-test-practice-shop/README.md"})
    void everyDocumentOnlyNumericMutationIsRefusedWithoutChangingTheYaml(String document) throws Exception {
        Path root = locateProjectRoot();
        String yaml = document.substring(0, document.indexOf('/')) + "/src/test/resources/"
                + (document.contains("auto-ex") ? "atf-auto-ex.yaml" : "atf-practice-shop.yaml");
        byte[] originalYaml = Files.readAllBytes(root.resolve(yaml));
        String source = read(root, document);
        String row = source.lines().filter(line -> line.startsWith("| YAML livrat |")).findFirst().orElseThrow();
        var numbers = Pattern.compile("[0-9]+(?:\\.[0-9]+)?").matcher(row).results().toList();
        assertEquals(6, numbers.size(), "Width, height, zoom, CSS width, CSS height and DPR remain separate claims");
        for (var number : numbers) {
            String mutated = row.substring(0, number.start()) + "9" + row.substring(number.start() + 1);
            String changed = source.replace(row, mutated);
            assertNotEquals(source, changed, "The mutant changes only its selected numeric declaration");
            var failures = inspect(root, document, changed);
            assertEquals(1, failures.size(), "Each independent field mutation must be detected: " + number.group());
            assertTrue(failures.getFirst().matches("(?s)DOC-FACT " + document + ":[0-9]+ .*declared=.*; observed=.*; source=.*"));
            assertTrue(failures.getFirst().contains(yaml), "The diagnostic names the independent source");
            assertArrayEquals(originalYaml, Files.readAllBytes(root.resolve(yaml)), "A documentation mutant cannot rewrite the authority");
        }
    }

    @Test void aConfiguredSeventyPercentExampleCannotReplaceTheGenericDefault() throws Exception {
        Path root = locateProjectRoot(); String document = DocumentedUiGeometryContracts.UI;
        String source = read(root, document);
        String changed = source.replace("Implicitul generic al `zoomPercent` este `100`", "Implicitul generic al `zoomPercent` este `70`");
        assertNotEquals(source, changed);
        var failures = inspect(root, document, changed);
        assertEquals(1, failures.size());
        assertTrue(failures.getFirst().contains("generic zoom default: declared=70; observed=100; source=atf-ui/src/main/java/atf/ui/config/UiBrowserConfiguration.java"));
    }

    @Test void aWrongDerivedCssValueInTheUiExampleIsRefused() throws Exception {
        Path root = locateProjectRoot(); String document = DocumentedUiGeometryContracts.UI;
        String source = read(root, document);
        String changed = source.replace("viewport CSS `2057 × 1429`", "viewport CSS `2058 × 1429`");
        assertNotEquals(source, changed);
        var failures = inspect(root, document, changed);
        assertEquals(1, failures.size());
        assertTrue(failures.getFirst().contains("; source=atf-ui/src/main/java/atf/ui/config/geometry/UiViewportScale.java"));
    }

    @ParameterizedTest @ValueSource(strings = {"```", "~~~~"})
    void missingDuplicateAndFencedClaimsCannotMakeTheGuardSilentlyPass(String fence) throws Exception {
        Path root = locateProjectRoot(); String document = "atf-test-auto-ex/README.md";
        String source = read(root, document);
        String row = source.lines().filter(line -> line.startsWith("| YAML livrat |")).findFirst().orElseThrow();
        assertTrue(inspect(root, document, source.replace(row, "")).getFirst().contains("missing or malformed declaration"));
        assertTrue(inspect(root, document, source.replace(row, row + "\n" + row)).getFirst().contains("duplicate declaration"));
        String onlyExample = source.replace(row, fence + "text\n" + row + "\n" + fence);
        assertTrue(inspect(root, document, onlyExample).getFirst().contains("missing or malformed declaration"));
        assertEquals(List.of(), inspect(root, document, source + "\n" + fence + "text\n" + row + "\n" + fence));
    }

    private static String read(Path root, String document) throws Exception { return Files.readString(root.resolve(document)); }
    private static List<String> inspect(Path root, String document, String content) throws Exception {
        return DocumentedUiGeometryContracts.inspect(root, document, content);
    }
}
