package atf.autoex.tests.e2e;

import atf.autoex.tests.extension.AutoExE2eTest;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.autoex.runtime.AutoExE2eRun;
import atf.autoex.support.journey.AutoExAccountJourney;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Real account self-service, independent of checkout and payment; adverts are never fabricated. */
@AutoExE2eTest
@AtfTestChannel(TestChannel.UI)

@DisplayName("Automation Exercise customer account — UI")
public class AutoExAccountUiE2eTest {
    @Test
    @DisplayName("a registered customer deletes their own account and returns to the store")
    void registersAndDeletesOwnAccount(AutoExE2eRun run) {
        AutoExAccountJourney.registersAndDeletesOwnAccount(run);
    }
}
