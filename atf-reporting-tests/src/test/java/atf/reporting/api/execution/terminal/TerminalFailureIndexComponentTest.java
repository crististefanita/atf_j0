package atf.reporting.api.execution.terminal;

import atf.reporting.support.failurecorrelation.TerminalFailureFixture;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.nio.file.Files;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class TerminalFailureIndexComponentTest {
    static Stream<Arguments> cases() {
        return Stream.of(false,true).flatMap(fallback->Stream.of(TerminalFailureFixture.Case.values()).map(kind->Arguments.of(fallback,kind)));
    }
    @ParameterizedTest @MethodSource("cases")
    void terminalCauseStaysTypedAndAuditableWithoutManufacturingAssertions(boolean fallback,TerminalFailureFixture.Case kind) throws Exception {
        var output=TerminalFailureFixture.publish(fallback,kind);
        String html=Files.readString(output);
        assertTrue(html.contains("data-atf-terminal-failure=\"true\""));
        assertTrue(html.contains("data-atf-request-path=\"/terminal\""));
        assertFalse(html.contains(TerminalFailureFixture.SECRET));
        if(fallback) {
            var section=java.util.regex.Pattern.compile("(?s)<section[^>]*id=\"fails\"[^>]*>(.*?)</section>").matcher(html);
            assertTrue(section.find());String fails=section.group(1);
            boolean expected=kind==TerminalFailureFixture.Case.EXPECTED_PASS||kind==TerminalFailureFixture.Case.EXPECTED_PASS_WITH_FAILED_SIBLING;
            assertEquals(!expected,fails.contains("Execution failure · GET /terminal"));
            int count=kind==TerminalFailureFixture.Case.EXPECTED_PASS?0:kind==TerminalFailureFixture.Case.WITH_ASSERTION?2:1;
            assertEquals(count,java.util.regex.Pattern.compile("<a\\b").matcher(fails).results().count());
        }
    }
}
