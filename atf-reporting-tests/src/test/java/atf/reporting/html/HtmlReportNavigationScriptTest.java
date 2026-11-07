package atf.reporting.html;
import static atf.reporting.support.navigation.NavigationScriptContract.assertTabBindings;
import atf.reporting.html.script.HtmlReportNavigationScript;
import atf.testkit.AtfUnitTest;

import atf.reporting.support.TestReportPaths;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.console.AtfConsoleSink;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class HtmlReportNavigationScriptTest {
    @Test
    void standardScriptOwnsOnlyFallbackReportNavigation() {
        String script = HtmlReportNavigationScript.standard().source();

        assertAll(
                () -> assertNotNull(script),
                () -> assertTabBindings(script),
                () -> assertTrue(script.contains("history.replaceState")),
                () -> assertTrue(script.contains("activate('overview')")),
                () -> assertTrue(script.contains("activate('tests')")));
    }

    @Test
    void sessionAcceptsAReplaceableScriptThroughItsAssetPort() {
        var session = new HtmlReportSession(
                TestReportPaths.report("navigation-port.html"),
                ReportingSettings.defaults(),
                AtfConsoleSink.noOp(),
                () -> "window.__atfNavigationPort = true;");

        assertTrue(session.render().contains("window.__atfNavigationPort = true;"));
    }
}
