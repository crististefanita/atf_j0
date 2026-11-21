package atf.api.tests.governance.maintenance;

import atf.api.tests.governance.distribution.GovernanceDeployment;
import atf.api.tests.governance.distribution.ReportingGovernanceSurface;
import atf.api.tests.governance.distribution.core.CoreGovernanceSurface;
import atf.core.config.module.AtfModuleIds;
import atf.libraries.PlatformLibrarySurface;
import atf.testkit.AtfIntegrationTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Read-only acceptance of a source workspace and its selected binary consumer. */
@AtfIntegrationTest
class AtfMaintenanceWorkspacePairGovernanceTest {
    private static final Pattern OTHER_WORKSPACE_REFERENCE = Pattern.compile("(?i)(?<![\\w])atf[-_]j(?:0|[2-9])(?!\\d)");

    @Test
    void producerAndConsumerKeepTheirDeclaredOwnership() throws Exception {
        Path j0 = workspace("atf.maintenance.j0");
        Path j1 = workspace("atf.maintenance.j1");

        var producer = GovernanceDeployment.read(j0);
        assertTrue(producer.reportingSource(), "J0 owns Reporting implementation sources");
        assertTrue(producer.sourceModules().contains(AtfModuleIds.TEST_KIT), "J0 owns Test Kit implementation sources");

        var consumer = GovernanceDeployment.read(j1);
        assertFalse(consumer.reportingSource(), "J1 consumes the published Reporting binary");
        assertFalse(consumer.sourceModules().contains(AtfModuleIds.TEST_KIT), "J1 consumes the published Test Kit binary");
        assertFalse(Files.exists(j1.resolve(AtfModuleIds.REPORTING)), "J1 has no shadow Reporting source module");
        assertFalse(Files.exists(j1.resolve(AtfModuleIds.TEST_KIT)), "J1 has no shadow Test Kit source module");
    }

    @Test
    void consumerUsesTheFiveVerifiedFlatLibraryPairs() throws Exception {
        Path j1 = workspace("atf.maintenance.j1");
        assertFalse(ReportingGovernanceSurface.read(j1).source());
        assertFalse(CoreGovernanceSurface.read(j1).source());
        for (String artifact : List.of("console", "mock", "test-kit")) {
            new PlatformLibrarySurface(j1, artifact).inspectConsumerArtifact();
        }
        PlatformLibrarySurface.assertFlatLayout(j1);
    }

    @Test
    void consumerDocumentationNamesLibrariesWithoutNamingOtherWorkspaces() throws Exception {
        Path j1 = workspace("atf.maintenance.j1");
        List<String> findings = new ArrayList<>();
        var libraryCatalog = new StringBuilder();
        try (Stream<Path> paths = Files.walk(j1)) {
            for (Path file : paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".md"))
                    .filter(path -> !excludedGeneratedPath(j1, path)).toList()) {
                String content = Files.readString(file);
                if (OTHER_WORKSPACE_REFERENCE.matcher(content).find()) findings.add(j1.relativize(file).toString());
            }
        }
        assertTrue(findings.isEmpty(), "consumer documentation must describe libraries without naming other ATF workspaces: " + findings);
        libraryCatalog.append(Files.readString(j1.resolve("docs/ATF_Workspace_Synchronization.md")));
        for (String artifact : List.of("core", "reporting", "console", "mock", "test-kit")) {
            String version = PlatformLibrarySurface.version(j1, artifact);
            assertTrue(libraryCatalog.toString().contains("atf.api:atf-" + artifact + ":" + version),
                    "consumer documentation names the pinned " + artifact + " library coordinate");
            assertTrue(libraryCatalog.toString().contains("lib/atf-" + artifact + "-" + version + ".jar"),
                    "consumer documentation names the pinned " + artifact + " JAR");
        }
    }

    private static Path workspace(String property) throws IOException {
        String configured = System.getProperty(property);
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("Set -D" + property + " to an absolute workspace path");
        }
        Path root = Path.of(configured);
        if (!root.isAbsolute()) throw new IllegalArgumentException(property + " must be absolute");
        root = root.toRealPath();
        if (!Files.isRegularFile(root.resolve("pom.xml"))) {
            throw new IllegalArgumentException(property + " is not a Maven workspace: " + root);
        }
        return root;
    }

    private static boolean excludedGeneratedPath(Path root, Path path) {
        Path relative = root.relativize(path);
        for (Path segment : relative) {
            String name = segment.toString();
            if (List.of(".git", ".idea", "target", "node_modules", "artifacts").contains(name)) return true;
        }
        return false;
    }
}
