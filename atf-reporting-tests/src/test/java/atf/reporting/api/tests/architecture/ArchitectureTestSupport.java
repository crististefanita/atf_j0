package atf.reporting.api.tests.architecture;

import atf.core.config.module.AtfModuleIds;

import java.nio.file.Files;
import java.nio.file.Path;

final class ArchitectureTestSupport {
    private ArchitectureTestSupport() {
    }

    static Path locateProjectRoot() {
        Path current = Path.of(System.getProperty("user.dir", "."))
                .toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isDirectory(current.resolve(AtfModuleIds.CORE))
                    && Files.isDirectory(current.resolve(AtfModuleIds.VALIDATION))
                    && Files.isRegularFile(current.resolve("pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        return Path.of(System.getProperty("user.dir", "."))
                .toAbsolutePath().normalize();
    }

}
