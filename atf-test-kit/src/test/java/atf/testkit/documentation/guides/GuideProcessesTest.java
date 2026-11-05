package atf.testkit.documentation.guides;

import atf.testkit.AtfUnitTest;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class GuideProcessesTest {
    @Test void childStdoutAndStderrAreUtf8EvenWithTheWindowsNativeCodePage(@TempDir Path work) throws Exception {
        String message = "șțăîâ — audit";
        var compiled = GuideCompilation.compile(work, "public class GuideExample { public static void main(String[] args) {"
                + " System.out.println(\"" + message + "\"); System.err.println(\"" + message + "\"); } }", List.of());
        assertTrue(compiled.passed(), compiled::diagnostics);
        String java = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        var result = GuideProcesses.run(work, "unicode", List.of(java, "-cp", compiled.classes().toString(), "GuideExample"));
        assertEquals(0, result.exit(), result::text);
        assertEquals(2, result.text().lines().filter(message::equals).count(), result::text);
    }
}
