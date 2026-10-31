package atf.reporting.runtime.core;

import atf.reporting.html.script.HtmlReportScriptModule;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Component contract for the semantic runtime module graph. */
@AtfUnitTest
class ExtentReportRuntimeModuleTest {
    private static final Pattern FUNCTION_DECLARATION =
            Pattern.compile("function\\s+([A-Za-z0-9_$]+)\\s*\\(");

    @Test
    void modulesExposeOneStablePublicationOwnerInSemanticOrder() {
        ExtentReportRuntimeModule[] modules = ExtentReportRuntimeModule.values();

        assertAll(
                () -> assertEquals(HtmlReportScriptModule.RUNTIME_CORE, modules[0].publicationOwner()),
                () -> assertEquals(HtmlReportScriptModule.RUNTIME_TESTS, modules[1].publicationOwner()),
                () -> assertEquals(HtmlReportScriptModule.RUNTIME_OVERVIEW, modules[2].publicationOwner()),
                () -> assertEquals(HtmlReportScriptModule.RUNTIME_EVIDENCE, modules[3].publicationOwner()),
                () -> assertEquals(HtmlReportScriptModule.RUNTIME_NAVIGATION, modules[4].publicationOwner()),
                () -> assertEquals(HtmlReportScriptModule.RUNTIME_DURATION, modules[5].publicationOwner()),
                () -> assertEquals(HtmlReportScriptModule.RUNTIME_FAILURES, modules[6].publicationOwner()),
                () -> assertEquals(HtmlReportScriptModule.RUNTIME_FAILURE_CAUSE, modules[7].publicationOwner()),
                () -> assertEquals(8, modules.length));
    }

    @Test
    void everyModulePublishesNonEmptyScriptAndOwnsUniqueFunctionDeclarations() {
        Map<String, String> ownersByFunction = new HashMap<>();

        for (ExtentReportRuntimeModule module : ExtentReportRuntimeModule.values()) {
            String script = module.script();
            Matcher matcher = FUNCTION_DECLARATION.matcher(script);
            Set<String> functions = new HashSet<>();

            assertFalse(script.isBlank(), () -> module + " must publish JavaScript");
            while (matcher.find()) {
                String function = matcher.group(1);
                assertTrue(functions.add(function),
                        () -> module + " declares function more than once: " + function);
                String previousOwner = ownersByFunction.put(function, module.name());
                assertEquals(null, previousOwner,
                        () -> "Function " + function + " is owned by both " + previousOwner
                                + " and " + module.name());
            }
            assertNotNull(module.publicationOwner());
        }

        assertTrue(ownersByFunction.containsKey("atfDurationValue"),
                "Duration must own its parser primitives");
        assertTrue(ownersByFunction.containsKey("normalizeEvidenceDisclosures"),
                "Evidence must own disclosure normalization");
        assertTrue(ownersByFunction.containsKey("populateAudit"),
                "Overview must own audit projection");
        assertTrue(ownersByFunction.containsKey("bindNodeCollapse"),
                "Navigation must own node interaction");
    }

}
