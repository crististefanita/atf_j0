package atf.autoex.page;

import atf.autoex.ui.AutoExHomePagePort;

/** Explicitly declines optional site tracking when a fresh browser is prompted. */
public final class PrivacyChoicesPage extends AutoExPage {
    private final AutoExHomePagePort run;
    public PrivacyChoicesPage(AutoExHomePagePort run) { super(run); this.run = run; }

    public void rejectOptionalProcessingIfPrompted() {
        run.dismissPrivacyPromptIfPresent();
    }
}
