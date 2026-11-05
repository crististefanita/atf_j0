package atf.console.guides;

import atf.testkit.AtfIntegrationTest;
import atf.testkit.documentation.guides.LibraryGuideConsumer;
import atf.testkit.files.AtfTestResourceReader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@AtfIntegrationTest
class ConsoleLibraryGuideIT {
    @Test void deliveredJavaAndYamlRespectConfigurationAndSelectionPrecedence(@TempDir Path work) throws Exception {
        var consumer = new LibraryGuideConsumer(work, "Console");
        Files.writeString(work.resolve("console.yaml"), consumer.guide().block("yaml"));
        String oracle = AtfTestResourceReader.readResourceUtf8(getClass(), "atf/console/guides/oracle.java.txt");
        var compiled = consumer.compile("nominal", false, oracle);
        assertTrue(compiled.passed(), compiled::diagnostics);
        String output = consumer.execute(compiled);
        assertTrue(output.contains("Catalogul de scenarii a fost încărcat"));
        assertTrue(output.contains("CONSOLE_GUIDE=PASS"));
        consumer.verifyRefusals(false, oracle, ".configuredStdout(");
    }
}
