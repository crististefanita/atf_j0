package atf.autoex.tests.e2e;

import atf.autoex.tests.extension.AutoExE2eTest;
import atf.autoex.tests.extension.AutoExNetworkDocuments;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.autoex.runtime.AutoExE2eRun;
import atf.autoex.support.journey.AutoExAccountJourney;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Same account business story as UI-only, with first-party browser exchanges attached. */
@AutoExE2eTest
@AtfTestChannel(TestChannel.UI_API)
@AutoExNetworkDocuments({"/"})
@DisplayName("Automation Exercise customer account — UI + API")
public class AutoExAccountUiApiE2eTest {
    @Test
    @DisplayName("a registered customer deletes their own account and returns to the store")
    void registersAndDeletesOwnAccount(AutoExE2eRun run) {
        AutoExAccountJourney.registersAndDeletesOwnAccount(run);
    }
}
