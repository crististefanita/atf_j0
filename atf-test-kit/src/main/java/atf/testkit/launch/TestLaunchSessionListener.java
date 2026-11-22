package atf.testkit.launch;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/** Service-loaded, read-only bridge over public JUnit Platform callbacks. */
public final class TestLaunchSessionListener implements LauncherSessionListener {
    private LaunchPlanListener listener;
    @Override public void launcherSessionOpened(LauncherSession session) {
        listener = new LaunchPlanListener();
        session.getLauncher().registerLauncherDiscoveryListeners(listener);
        session.getLauncher().registerTestExecutionListeners(listener);
    }
    @Override public void launcherSessionClosed(LauncherSession session) {
        if (listener != null) listener.close();
    }
}
