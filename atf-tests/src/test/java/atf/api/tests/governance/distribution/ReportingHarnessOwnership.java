package atf.api.tests.governance.distribution;

import atf.core.config.module.AtfModuleIds;
import atf.core.files.AtfJavaSourceReferences;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/** Source audits follow declared ownership; binary consumers must contain no harness or copied namespaces. */
public final class ReportingHarnessOwnership {
    @FunctionalInterface public interface Inspection { void verify() throws Exception; }
    private static final Pattern HARNESS_PACKAGE = Pattern.compile(
            "\\bpackage\\s+(?:atf\\.reporting|atf\\.ui\\.tests\\.extent)(?:\\.|\\s*;)");
    private final GovernanceDeployment deployment;
    private ReportingHarnessOwnership(GovernanceDeployment deployment) { this.deployment = deployment; }
    public static ReportingHarnessOwnership read(Path root) throws Exception {
        return new ReportingHarnessOwnership(GovernanceDeployment.read(root));
    }
    public void inspect(Inspection inspection) throws Exception {
        if (deployment.sourceModules().contains(AtfModuleIds.REPORTING_TESTS)) inspection.verify();
        else verifyExternalOwnership();
    }
    public void requireFile(Path file) {
        Path owner = deployment.root().resolve(AtfModuleIds.REPORTING_TESTS);
        if (!file.normalize().startsWith(owner)) throw new IllegalArgumentException("Not a Reporting harness path: " + file);
        assertEquals(deployment.sourceModules().contains(AtfModuleIds.REPORTING_TESTS), Files.isRegularFile(file),
                () -> "Declared Reporting harness ownership: " + file);
    }
    private void verifyExternalOwnership() throws Exception {
        assertFalse(Files.exists(deployment.root().resolve(AtfModuleIds.REPORTING_TESTS)),
                "External Reporting harness must not remain in the consumer");
        for (String module : deployment.sourceModules()) {
            Path sources = deployment.root().resolve(module).resolve("src");
            if (!Files.isDirectory(sources)) continue;
            try (var files = Files.walk(sources)) {
                for (Path file : files.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".java")).toList()) {
                    String code = AtfJavaSourceReferences.codeOnly(Files.readString(file)).replaceAll("\\s*\\.\\s*", ".");
                    assertFalse(HARNESS_PACKAGE.matcher(code).find(), () -> "Reporting harness namespace copied into consumer: " + file);
                }
            }
        }
    }
}
