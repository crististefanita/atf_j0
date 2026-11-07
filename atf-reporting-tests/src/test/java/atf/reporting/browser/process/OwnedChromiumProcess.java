package atf.reporting.browser.process;

import com.google.gson.JsonElement;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.StreamSupport;

/** Native termination is the release operation for a worker's disposable Windows Chromium. */
public final class OwnedChromiumProcess implements AutoCloseable {
    private final ProcessHandle root;
    private final ProcessHandle driver;

    private OwnedChromiumProcess(ProcessHandle root, ProcessHandle driver) {
        this.root = root;
        this.driver = driver;
    }

    public static boolean supports(BrowserType.LaunchOptions options) {
        return System.getProperty("os.name", "").startsWith("Windows")
                && !Boolean.FALSE.equals(options.headless) && options.channel == null && options.executablePath == null;
    }

    public static OwnedChromiumProcess claim(Browser browser) {
        var session = browser.newBrowserCDPSession();
        long pid;
        try {
            var entries = session.send("SystemInfo.getProcessInfo").getAsJsonArray("processInfo");
            var browserPids = StreamSupport.stream(entries.spliterator(), false).map(JsonElement::getAsJsonObject)
                    .filter(entry -> entry.get("type").getAsString().equals("browser"))
                    .map(entry -> entry.get("id").getAsLong()).toList();
            if (browserPids.size() != 1) throw new IllegalStateException("Expected one native Chromium root: " + browserPids);
            pid = browserPids.getFirst();
        } finally {
            session.detach();
        }
        ProcessHandle worker = ProcessHandle.current();
        ProcessHandle root = ownedDescendant(pid, worker);
        ProcessHandle driver = root.parent().filter(handle -> handle.parent().filter(worker::equals).isPresent())
                .filter(OwnedChromiumProcess::isNode)
                .orElseThrow(() -> new IllegalStateException("Chromium PID " + pid + " has no owned Playwright driver"));
        return new OwnedChromiumProcess(root, driver);
    }

    static ProcessHandle ownedDescendant(long pid, ProcessHandle worker) {
        try (var children = worker.descendants()) {
            return children.filter(handle -> handle.pid() == pid && handle.isAlive()).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Chromium PID " + pid + " is not a live descendant of worker " + worker.pid()));
        }
    }

    private static boolean isNode(ProcessHandle handle) {
        return handle.info().command().map(Path::of).map(Path::getFileName).map(Path::toString)
                .filter("node.exe"::equalsIgnoreCase).isPresent();
    }

    public void closeDriver() throws InterruptedException, ExecutionException {
        terminateTree(driver, "Playwright driver");
    }

    @Override public void close() throws InterruptedException, ExecutionException {
        terminateTree(root, "Chromium");
    }

    private static void terminateTree(ProcessHandle root, String label) throws InterruptedException, ExecutionException {
        if (!root.isAlive()) throw new IllegalStateException("Owned " + label + " exited before teardown: " + root.pid());
        List<ProcessHandle> handles;
        try (var descendants = root.descendants()) {
            handles = new ArrayList<>(descendants.toList());
        }
        handles.addFirst(root);
        var refused = new ArrayList<Long>();
        for (ProcessHandle handle : handles) {
            if (handle.isAlive() && !handle.destroyForcibly() && handle.isAlive()) refused.add(handle.pid());
        }
        if (!refused.isEmpty()) throw new IllegalStateException("Owned " + label + " termination refused for PIDs " + refused);
        for (ProcessHandle handle : handles) handle.onExit().get();
        System.out.println("ATF_OWNED_EXIT " + label + " pid=" + root.pid() + " processes=" + handles.size());
    }
}
