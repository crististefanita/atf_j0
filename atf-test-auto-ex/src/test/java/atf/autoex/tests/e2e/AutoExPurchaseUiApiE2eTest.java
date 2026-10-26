package atf.autoex.tests.e2e;

import atf.autoex.tests.extension.AutoExE2eTest;
import atf.autoex.tests.extension.AutoExNetworkDocuments;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.autoex.runtime.AutoExE2eRun;
import atf.autoex.support.journey.AutoExPurchaseJourney;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

/** Same purchase journey, augmented with redacted API exchanges from its browser session. */
@AutoExE2eTest
@AtfTestChannel(TestChannel.UI_API)
@AutoExNetworkDocuments({"/"})

@Order(2)
@DisplayName("Automation Exercise purchase — UI + API")
public class AutoExPurchaseUiApiE2eTest {
    @Test
    @DisplayName("customer completes the same demo purchase with UI and API evidence")
    void completesPurchaseUsingUiAndApiEvidence(AutoExE2eRun run) {
        AutoExPurchaseJourney.execute(run);
    }
}
