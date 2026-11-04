package atf.api.tests.governance.distribution.core;

import atf.core.config.module.AtfModuleIds;
import atf.api.tests.governance.distribution.ReportingGovernanceSurface;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/** Private synthetic binary for negative controls, never a replacement for live CodeSource acceptance. */
final class BinaryCoreFixture {
    final Path root, jar, pom;
    BinaryCoreFixture(Path root) throws Exception {
        this.root = root;
        var modules = AtfModuleIds.REACTOR_MODULES.stream()
                .filter(n -> !java.util.List.of("atf-core", "atf-reporting", "atf-reporting-tests").contains(n)).toList();
        for (String module : modules) { Files.createDirectories(root.resolve(module)); Files.writeString(root.resolve(module + "/pom.xml"), "<project/>"); }
        Files.writeString(root.resolve("pom.xml"), "<project><properties><atf.core.version>fixture</atf.core.version>"
                + "<atf.reporting.version>fixture</atf.reporting.version></properties><modules>"
                + String.join("", modules.stream().map(n -> "<module>" + n + "</module>").toList()) + "</modules></project>");
        Files.createDirectories(root.resolve("lib"));
        Files.writeString(root.resolve("lib/README.md"), "# Library ownership\n");
        jar = root.resolve("lib/atf-core-fixture.jar"); pom = root.resolve("lib/atf-core-fixture.pom");
        Files.writeString(root.resolve("lib/atf-reporting-fixture.jar"), "other artifact");
        Files.writeString(root.resolve("lib/atf-reporting-fixture.pom"), "other POM");
        Files.writeString(pom, "<project><groupId>atf.api</groupId><artifactId>atf-core</artifactId><version>fixture</version></project>");
        pack(false);
    }
    void pack(boolean hiddenDependency) throws Exception {
        Path source = root.resolve("fixture-source/atf/core/config/module/AtfModuleIds.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "package atf.core.config.module; public class AtfModuleIds {"
                + (hiddenDependency ? " java.sql.Connection forbidden; " : "") + "}");
        Path classes = root.resolve("fixture-classes"); Files.createDirectories(classes);
        if (javax.tools.ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", classes.toString(), source.toString()) != 0) throw new IllegalStateException("Fixture compilation failed");
        try (var out = new JarOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new JarEntry("META-INF/maven/atf.api/atf-core/pom.xml")); out.write(Files.readAllBytes(pom)); out.closeEntry();
            out.putNextEntry(new JarEntry("atf/core/config/module/AtfModuleIds.class"));
            out.write(Files.readAllBytes(classes.resolve("atf/core/config/module/AtfModuleIds.class"))); out.closeEntry();
        }
        var mapper = new ObjectMapper(); var metadata = mapper.createObjectNode(); var core = metadata.putObject("coreLibrary");
        core.put("coordinate", "atf.api:atf-core:fixture"); core.put("binarySha256", ReportingGovernanceSurface.hash(jar));
        core.put("publishedPomSha256", ReportingGovernanceSurface.hash(pom));
        var inputs = core.putObject("producerInputs"); inputs.put("files", 1); inputs.put("sha256", "a".repeat(64));
        inputs.putArray("nativeTests").addObject().put("path", "src/test/java/NativeTest.java").put("sha256", "b".repeat(64));
        core.put("producerInputsSha256", digest(inputs)); mapper.writeValue(root.resolve("library-provenance.json").toFile(), metadata);
    }
    static String digest(ObjectNode node) throws Exception {
        return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(node.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
