package atf.testkit.documentation.guides;

import atf.testkit.AtfIntegrationTest;
import atf.testkit.files.AtfTestResourceReader;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@AtfIntegrationTest
class KitLibraryGuideIT {
    @Test void deliveredGuideRetainsJUnitFailureAndRejectsApiOrBinaryDrift(@TempDir Path work) throws Exception {
        var consumer = new LibraryGuideConsumer(work, "TestKit");
        String oracle = AtfTestResourceReader.readResourceUtf8(getClass(), "atf/testkit/documentation/guides/oracle.java.txt");
        var compiled = consumer.compile("nominal", true, oracle);
        assertTrue(compiled.passed(), compiled::diagnostics);
        assertTrue(consumer.execute(compiled).contains("TESTKIT_GUIDE=PASS"));
        consumer.verifyRefusals(true, oracle, ".equals(");
        var withoutJUnit = consumer.classpath().stream().filter(path -> !path.getFileName().toString().startsWith("junit-jupiter-api-")).toList();
        var refusal = consumer.compile("missing-junit-api", consumer.guide().executable(true, oracle,
                "private static void assertBinary() { }"), withoutJUnit);
        assertFalse(refusal.passed(), "The child compiler must not borrow JUnit from its host");
    }
}
