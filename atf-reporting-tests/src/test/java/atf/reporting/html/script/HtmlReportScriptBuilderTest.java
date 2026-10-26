package atf.reporting.html.script;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest
class HtmlReportScriptBuilderTest {
    @Test
    void keepsNamedFragmentsInspectableAndOutputDeterministic() {
        var builder = new HtmlReportScriptBuilder()
                .append("first", "const a=1;")
                .append("second", "const b=2;");

        assertEquals("const a=1;const b=2;", builder.build());
        assertEquals("first", builder.fragments().get(0).owner());
        assertEquals("second", builder.fragments().get(1).owner());
    }

    @Test
    void rejectsUnnamedFragments() {
        assertThrows(IllegalArgumentException.class,
                () -> new HtmlReportScriptBuilder().append(" ", "const a=1;"));
    }

    @Test
    void keepsRuntimeOwnersTypedAlongsideFallbackOwners() {
        var builder = new HtmlReportScriptBuilder()
                .append(HtmlReportScriptModule.RUNTIME_CORE, "function q(){}");

        assertEquals("runtime-core", builder.fragments().get(0).owner());
        assertEquals("function q(){}", builder.build());
    }
}
