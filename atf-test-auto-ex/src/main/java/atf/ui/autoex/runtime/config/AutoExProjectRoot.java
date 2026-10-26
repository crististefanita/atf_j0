package atf.ui.autoex.runtime.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Locates this consumer's reactor root without depending on IntelliJ's working directory. */
public final class AutoExProjectRoot {
    private static final String MODULE_MARKER = "<module>atf-test-auto-ex</module>";

    private AutoExProjectRoot() { }

    public static Path discover() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (current != null) {
            Path pom = current.resolve("pom.xml");
            if (Files.isRegularFile(pom) && declaresConsumer(pom)) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate the ATF reactor root for atf-test-auto-ex");
    }

    private static boolean declaresConsumer(Path pom) {
        try {
            return Files.readString(pom).contains(MODULE_MARKER);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot inspect candidate reactor POM: " + pom, error);
        }
    }
}
