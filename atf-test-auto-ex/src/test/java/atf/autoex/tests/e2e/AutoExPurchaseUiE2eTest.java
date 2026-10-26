package atf.autoex.tests.e2e;

import atf.autoex.tests.extension.AutoExE2eTest;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.autoex.runtime.AutoExE2eRun;
import atf.autoex.support.journey.AutoExPurchaseJourney;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

/** Complete purchase journey; verdicts and evidence come from the rendered UI. */
@AutoExE2eTest
@AtfTestChannel(TestChannel.UI)

@Order(1)
@DisplayName("Automation Exercise purchase — UI")
public class AutoExPurchaseUiE2eTest {
    @Test
    @DisplayName("customer registers during checkout, completes a demo order and removes the account")
    void completesPurchaseUsingUiEvidence(AutoExE2eRun run) {
        AutoExPurchaseJourney.execute(run);
    }
}
