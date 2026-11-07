package atf.reporting.browser.lifecycle;

import java.nio.file.Path;

/** Test-only lifecycle, executed on one thread in an exclusively owned JVM. */
public interface BrowserProbe extends AutoCloseable {
    void open(Path directory) throws Exception;
    void verify(String[] arguments) throws Exception;
    @Override void close() throws Exception;
}
