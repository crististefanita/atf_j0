package atf.api.tests.governance.documentation.factual;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import atf.api.tests.governance.AtfGovernanceTestSupport;
import atf.api.tests.governance.documentation.language.DocumentationLanguagePolicy;
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
    @TempDir Path fixtures;
    @Test void selectedClaimsMatchLiveYamlAndTheExecutableUiGeometry() throws Exception {
        Path root = locateProjectRoot();
        for (String document : DocumentedUiGeometryContracts.DOCUMENTS) {
            if (!document.equals(DocumentedUiGeometryContracts.UI) && !AtfGovernanceTestSupport.consumerDeclared(root, document.substring(0, document.indexOf('/')))) continue;
            assertEquals(List.of(), inspect(root, document, read(root, document)), document);
        }
    }

    @ParameterizedTest @ValueSource(strings = {"atf-test-auto-ex/README.md", "atf-test-practice-shop/README.md"})
    void everyDocumentOnlyNumericMutationIsRefusedWithoutChangingTheYaml(String document) throws Exception {
        Path root = authority(document);
        String yaml = document.substring(0, document.indexOf('/')) + "/src/test/resources/"
                + (document.contains("auto-ex") ? "atf-auto-ex.yaml" : "atf-practice-shop.yaml");
        byte[] originalYaml = Files.readAllBytes(root.resolve(yaml));
        String source = read(root, document);
        String row = deliveredRow(root, source);
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
        String language = DocumentationLanguagePolicy.read(root);
        var claim = DocumentedUiGeometryContracts.defaultPattern(language).matcher(source);
        assertTrue(claim.find());
        String changed = source.substring(0, claim.start(1)) + "70" + source.substring(claim.end(1));
        assertNotEquals(source, changed);
        var failures = inspect(root, document, changed);
        assertEquals(1, failures.size());
        assertTrue(failures.getFirst().contains("generic zoom default: declared=70; observed=100; source=atf-ui/src/main/java/atf/ui/config/UiBrowserConfiguration.java"));
    }

    @Test void aWrongDerivedCssValueInTheUiExampleIsRefused() throws Exception {
        Path root = locateProjectRoot(); String document = DocumentedUiGeometryContracts.UI;
        String source = read(root, document);
        String language = DocumentationLanguagePolicy.read(root);
        var claim = DocumentedUiGeometryContracts.examplePattern(language).matcher(source);
        assertTrue(claim.find());
        String wrongWidth = Integer.toString(Integer.parseInt(claim.group(4)) + 1);
        String changed = source.substring(0, claim.start(4)) + wrongWidth + source.substring(claim.end(4));
        assertNotEquals(source, changed);
        var failures = inspect(root, document, changed);
        assertEquals(1, failures.size());
        assertTrue(failures.getFirst().contains("; source=atf-ui/src/main/java/atf/ui/config/geometry/UiViewportScale.java"));
    }

    @ParameterizedTest @ValueSource(strings = {"```", "~~~~"})
    void missingDuplicateAndFencedClaimsCannotMakeTheGuardSilentlyPass(String fence) throws Exception {
        String document = "atf-test-auto-ex/README.md"; Path root = authority(document);
        String source = read(root, document);
        String row = deliveredRow(root, source);
        assertTrue(inspect(root, document, source.replace(row, "")).getFirst().contains("missing or malformed declaration"));
        assertTrue(inspect(root, document, source.replace(row, row + "\n" + row)).getFirst().contains("duplicate declaration"));
        String onlyExample = source.replace(row, fence + "text\n" + row + "\n" + fence);
        assertTrue(inspect(root, document, onlyExample).getFirst().contains("missing or malformed declaration"));
        assertEquals(List.of(), inspect(root, document, source + "\n" + fence + "text\n" + row + "\n" + fence));
    }

    private static String deliveredRow(Path root, String source) throws Exception {
        var pattern = DocumentedUiGeometryContracts.deliveredPattern(DocumentationLanguagePolicy.read(root));
        return source.lines().filter(line -> pattern.matcher(line).matches()).findFirst().orElseThrow();
    }
    private Path authority(String document) throws Exception {
        Path live = locateProjectRoot(); String module = document.substring(0, document.indexOf('/'));
        if (AtfGovernanceTestSupport.consumerDeclared(live, module)) return live;
        Path target = fixtures.resolve(document); Files.createDirectories(target.getParent());
        Files.writeString(target, "| YAML livrat | `1000 × 800` | `100` | `1000 × 800` | `1` |\n");
        Path yaml = fixtures.resolve(module + "/src/test/resources/" + (module.contains("auto-ex") ? "atf-auto-ex.yaml" : "atf-practice-shop.yaml"));
        Files.createDirectories(yaml.getParent());
        Files.writeString(yaml, module.contains("auto-ex") ? "automationExercise:\n  browser:\n    viewport: {width: 1000, height: 800}\n    zoomPercent: 100\n" : "practiceShop:\n  viewport: {width: 1000, height: 800}\n  zoomPercent: 100\n");
        return fixtures;
    }

    private static String read(Path root, String document) throws Exception { return Files.readString(root.resolve(document)); }
    private static List<String> inspect(Path root, String document, String content) throws Exception {
        return DocumentedUiGeometryContracts.inspect(root, document, content);
    }
}
