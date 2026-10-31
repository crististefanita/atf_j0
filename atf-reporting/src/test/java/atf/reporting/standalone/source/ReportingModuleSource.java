package atf.reporting.standalone.source;

import java.nio.file.Files;
import java.nio.file.Path;

/** Locates only the producer source belonging to these native test classes. */
public final class ReportingModuleSource {
    private ReportingModuleSource() { }
    public static Path root() throws Exception {
        Path current = Path.of(ReportingModuleSource.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isDirectory(current.resolve("src/main/java/atf/reporting"))
                    && Files.isRegularFile(current.resolve("pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate reporting module from its test CodeSource");
    }
}
