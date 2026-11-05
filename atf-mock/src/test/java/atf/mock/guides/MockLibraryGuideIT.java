package atf.mock.guides;

import atf.testkit.AtfIntegrationTest;
import atf.testkit.documentation.guides.LibraryGuideConsumer;
import atf.testkit.files.AtfTestResourceReader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@AtfIntegrationTest
class MockLibraryGuideIT {
    @Test void deliveredJavaUsesItsYamlCatalogAndExplicitFallbackOffline(@TempDir Path work) throws Exception {
        var consumer = new LibraryGuideConsumer(work, "Mock");
        Files.writeString(work.resolve("mock.yaml"), consumer.guide().block("yaml"));
        String oracle = AtfTestResourceReader.readResourceUtf8(getClass(), "atf/mock/guides/oracle.java.txt");
        var compiled = consumer.compile("nominal", false, oracle);
        assertTrue(compiled.passed(), compiled::diagnostics);
        assertTrue(consumer.execute(compiled).contains("MOCK_GUIDE=PASS"));
        consumer.verifyRefusals(false, oracle, ".fromConfiguration(");
    }
}
