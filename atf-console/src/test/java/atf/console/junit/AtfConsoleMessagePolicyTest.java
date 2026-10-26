package atf.console.junit;

import atf.console.ConsoleSettings;
import atf.testkit.AtfConsoleMessages;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@AtfUnitTest
class AtfConsoleMessagePolicyTest {
    @Test
    void inheritPreservesConfiguredConsoleSettings() {
        var configured = new ConsoleSettings(true, false);
        assertEquals(configured, AtfConsoleTestExtension.applyPolicy(
                configured, AtfConsoleMessages.Policy.INHERIT));
    }

    @Test
    void detailsAndSummaryRespectTheGlobalEnabledSwitch() {
        assertEquals(new ConsoleSettings(true, true), AtfConsoleTestExtension.applyPolicy(
                new ConsoleSettings(true, false), AtfConsoleMessages.Policy.DETAILS));
        assertEquals(new ConsoleSettings(true, false), AtfConsoleTestExtension.applyPolicy(
                new ConsoleSettings(true, true), AtfConsoleMessages.Policy.SUMMARY));
        assertEquals(new ConsoleSettings(false, true), AtfConsoleTestExtension.applyPolicy(
                new ConsoleSettings(false, false), AtfConsoleMessages.Policy.DETAILS));
    }

    @Test
    void offSuppressesConsoleProjection() {
        assertEquals(ConsoleSettings.disabled(), AtfConsoleTestExtension.applyPolicy(
                ConsoleSettings.defaults(), AtfConsoleMessages.Policy.OFF));
    }
}
