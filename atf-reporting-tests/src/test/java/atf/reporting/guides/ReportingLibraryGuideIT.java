package atf.reporting.guides;

import atf.testkit.AtfIntegrationTest;
import atf.testkit.documentation.guides.LibraryGuideConsumer;
import atf.testkit.files.AtfTestResourceReader;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@AtfIntegrationTest
class ReportingLibraryGuideIT {
    @Test void deliveredGuidePublishesAuditablePassFailAndNullWithoutCoreOrKit(@TempDir Path work) throws Exception {
        var consumer = new LibraryGuideConsumer(work, "Reporting");
        String oracle = AtfTestResourceReader.readResourceUtf8(getClass(), "atf/reporting/guides/oracle.java.txt");
        var compiled = consumer.compile("nominal", true, oracle);
        assertTrue(compiled.passed(), compiled::diagnostics);
        String output = consumer.execute(compiled);
        assertTrue(output.contains("REPORTING_GUIDE=PASS; cases=PASS,FAIL,NULL; preview=240"));
        assertTrue(output.contains("REPORTING_GUIDE_SHARED=PASS; invocations=2; artifact=1"));
        assertTrue(consumer.classpath().stream().noneMatch(file -> file.getFileName().toString().startsWith("atf-core-")));
        assertTrue(consumer.classpath().stream().noneMatch(file -> file.getFileName().toString().startsWith("atf-test-kit-")));
        consumer.verifyRefusals(true, oracle, ".open(");
    }
}
