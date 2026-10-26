package atf.autoex.ui;

import atf.autoex.config.AutoExConfiguration;

import java.net.URI;

/** Home-page capabilities; it does not expose report, API-journal or account-lifecycle controls. */
public interface AutoExHomePagePort extends AutoExUiActions {
    AutoExConfiguration configuration();
    void navigate(URI destination);
    boolean dismissPrivacyPromptIfPresent();
}
