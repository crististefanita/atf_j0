package atf.api.tests.governance.distribution;

import atf.core.http.request.ApiRequest;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.AtfReportPorts;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/** A private packaged fixture: mutations affect no live JAR, cache or producer source. */
final class BinaryReportingFixture {
    final Path root, jar, pom, manifest;
    private String extraDependency = "";
    private boolean hiddenReference;

    BinaryReportingFixture(Path root) throws Exception {
        this.root = root;
        DeploymentFixture.prepare(root, false);
        Path lib = root.resolve("lib"); Files.createDirectories(lib);
        jar = lib.resolve("atf-reporting-fixture.jar"); pom = lib.resolve("atf-reporting-fixture.pom");
        manifest = root.resolve("reporting-source-manifest.json");
        Files.writeString(manifest, "[{\"Path\":\"src/test/java/fixture/NativeTest.java\",\"Hash\":\"fixture\"}]");
        publish();
    }
    void withAtfDependency() throws Exception {
        extraDependency = "<dependency><groupId>atf.api</groupId><artifactId>atf-core</artifactId><version>1</version></dependency>";
        publish();
    }
    void withHiddenReference() throws Exception { hiddenReference = true; publish(); }
    private void publish() throws Exception {
        String xml = "<project><groupId>atf.api</groupId><artifactId>atf-reporting</artifactId><version>fixture</version><dependencies>"
                + extraDependency + "</dependencies></project>";
        Files.writeString(pom, xml);
        try (var out = new JarOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new JarEntry("META-INF/maven/atf.api/atf-reporting/pom.xml")); out.write(Files.readAllBytes(pom)); out.closeEntry();
            for (Class<?> type : hiddenReference ? List.of(ReportingSettings.class, AtfReportPorts.class, HiddenReference.class)
                    : List.of(ReportingSettings.class, AtfReportPorts.class)) {
                String name = type.getName().replace('.', '/') + ".class";
                try (var input = type.getResourceAsStream("/" + name)) {
                    out.putNextEntry(new JarEntry(name)); out.write(input.readAllBytes()); out.closeEntry();
                }
            }
        }
        var metadata = new ObjectMapper().createObjectNode();
        metadata.put("coordinate", "atf.api:atf-reporting:fixture");
        metadata.put("binarySha256", ReportingGovernanceSurface.hash(jar));
        metadata.put("publishedPomSha256", ReportingGovernanceSurface.hash(pom));
        metadata.put("producerManifestSha256", ReportingGovernanceSurface.hash(manifest));
        new ObjectMapper().writeValue(root.resolve("library-provenance.json").toFile(), metadata);
    }
    private static final class HiddenReference { ApiRequest request; }
}
