package atf.api.tests.governance.distribution;

import atf.core.config.module.AtfModuleIds;
import java.nio.file.Files;
import java.nio.file.Path;

/** Minimal owned Maven fixture shared by the source and binary negative controls. */
final class DeploymentFixture {
    private DeploymentFixture() { }
    static void prepare(Path root, boolean reportingSource) throws Exception {
        var modules = AtfModuleIds.REACTOR_MODULES.stream().filter(n -> reportingSource || !n.equals("atf-reporting")).toList();
        for (String module : modules) {
            Files.createDirectories(root.resolve(module)); Files.writeString(root.resolve(module).resolve("pom.xml"), "<project/>");
        }
        if (!reportingSource && Files.exists(root.resolve("atf-reporting/pom.xml"))) {
            Files.delete(root.resolve("atf-reporting/pom.xml")); Files.delete(root.resolve("atf-reporting"));
        }
        Files.writeString(root.resolve("pom.xml"), "<project><properties><atf.reporting.version>fixture</atf.reporting.version></properties><modules>"
                + String.join("", modules.stream().map(n -> "<module>" + n + "</module>").toList()) + "</modules></project>");
    }
}
