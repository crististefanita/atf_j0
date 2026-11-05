package atf.core.guides;

import atf.testkit.AtfIntegrationTest;
import atf.testkit.documentation.guides.LibraryGuideConsumer;
import atf.testkit.files.AtfTestResourceReader;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@AtfIntegrationTest
class CoreLibraryGuideIT {
    @Test void deliveredGuideExecutesStrictJsonAndRejectsApiOrDependencyDrift(@TempDir Path work) throws Exception {
        var consumer = new LibraryGuideConsumer(work, "Core");
        String oracle = AtfTestResourceReader.readResourceUtf8(getClass(), "atf/core/guides/oracle.java.txt");
        var compiled = consumer.compile("nominal", false, oracle);
        assertTrue(compiled.passed(), compiled::diagnostics);
        assertTrue(consumer.execute(compiled).contains("CORE_GUIDE=PASS"));
        consumer.verifyRefusals(false, oracle, ".of(");
        var withoutJackson = consumer.classpath().stream()
                .filter(file -> !file.getFileName().toString().startsWith("jackson-databind-")).toList();
        assertTrue(withoutJackson.size() < consumer.classpath().size());
        var refusal = consumer.compile("missing-transitive", consumer.guide().executable(false, oracle,
                "private static void assertBinary() { }"), withoutJackson);
        assertFalse(refusal.passed(), "The guide must not obtain Jackson classes from the host reactor");
        assertTrue(refusal.diagnostics().contains("JsonNode"), refusal::diagnostics);
    }
}
