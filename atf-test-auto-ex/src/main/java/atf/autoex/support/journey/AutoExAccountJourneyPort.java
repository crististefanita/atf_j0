package atf.autoex.support.journey;

import atf.autoex.support.trace.AutoExTraceStep;
import atf.autoex.ui.AutoExAccountPagePort;
import atf.autoex.ui.AutoExHomePagePort;
import atf.reporting.contract.AtfAssertion;
import java.util.List;

/** Account self-service capabilities, shared by account and signed-in cart journeys. */
public interface AutoExAccountJourneyPort extends AutoExAccountPagePort, AutoExHomePagePort {
    void finalScreenshot(AutoExTraceStep step, List<AtfAssertion> assertions);
}
