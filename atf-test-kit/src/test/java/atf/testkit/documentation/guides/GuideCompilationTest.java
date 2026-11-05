package atf.testkit.documentation.guides;

import atf.testkit.AtfUnitTest;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class GuideCompilationTest {
    @TempDir Path work;

    @Test void emptyExplicitClasspathCannotInheritJUnitOrKitFromTheTestRunner() throws Exception {
        var result = GuideCompilation.compile(work, "import atf.testkit.AtfUnitTest; public class GuideExample {}", List.of());
        assertFalse(result.passed(), "Host production classes must never satisfy the consumer compiler");
        assertTrue(result.diagnostics().contains("atf.testkit"));
    }

    @Test void validJavaCompilesAndSyntaxDriftIsNotConvertedIntoSuccess() throws Exception {
        assertTrue(GuideCompilation.compile(work.resolve("valid"), "public class GuideExample {}", List.of()).passed());
        assertFalse(GuideCompilation.compile(work.resolve("invalid"), "public class GuideExample { withdrawn( }", List.of()).passed());
    }
}
