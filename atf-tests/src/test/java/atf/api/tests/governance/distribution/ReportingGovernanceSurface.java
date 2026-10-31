package atf.api.tests.governance.distribution;

import atf.reporting.configuration.ReportingSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.spi.ToolProvider;

/** Inspects the owned source or the exact published JAR, without recreating Java source. */
public final class ReportingGovernanceSurface {
    private final GovernanceDeployment deployment;
    private final Path jar;
    private final Path pom;
    private final JsonNode producerInputs;

    private ReportingGovernanceSurface(GovernanceDeployment deployment) throws Exception {
        this.deployment = deployment;
        if (deployment.reportingSource()) { jar = null; pom = deployment.root().resolve("atf-reporting/pom.xml"); producerInputs = null; }
        else {
            String version = deployment.reportingVersion();
            if (!version.matches("[A-Za-z0-9][A-Za-z0-9._-]*")) throw new IllegalStateException("GOV-BINARY-VERSION: unsafe coordinate");
            Path base = deployment.root().resolve("lib");
            jar = base.resolve("atf-reporting-" + version + ".jar");
            pom = base.resolve("atf-reporting-" + version + ".pom");
            var provenance = new ObjectMapper().readTree(deployment.root().resolve("library-provenance.json").toFile());
            require(provenance.path("coordinate").asText().equals("atf.api:atf-reporting:" + version), "GOV-BINARY-COORDINATE");
            require(hash(jar).equalsIgnoreCase(provenance.path("binarySha256").asText()), "GOV-BINARY-HASH");
            require(hash(pom).equalsIgnoreCase(provenance.path("publishedPomSha256").asText()), "GOV-BINARY-POM-HASH");
            producerInputs = provenance.path("producerInputs");
            require(hashJson(producerInputs).equalsIgnoreCase(provenance.path("producerInputsSha256").asText()),
                    "GOV-BINARY-PRODUCER-INPUTS-HASH");
            require(producerInputs.path("files").asInt() > 0
                    && producerInputs.path("sha256").asText().matches("[A-Fa-f0-9]{64}"), "GOV-BINARY-PRODUCER-INPUTS");
            var nativeTests = producerInputs.path("nativeTests");
            require(nativeTests.isArray() && !nativeTests.isEmpty(), "GOV-BINARY-NATIVE-TESTS");
            var nativePaths = new java.util.HashSet<String>();
            for (var entry : nativeTests) {
                String name = entry.path("path").asText();
                require(name.startsWith("src/test/java/") && name.endsWith(".java") && !name.contains("..")
                        && name.indexOf('\\') < 0 && nativePaths.add(name)
                        && entry.path("sha256").asText().matches("[A-Fa-f0-9]{64}"), "GOV-BINARY-NATIVE-TESTS");
            }
            var project = GovernanceDeployment.xml(Files.readString(pom));
            require(GovernanceDeployment.text(project, "artifactId").equals("atf-reporting")
                    && GovernanceDeployment.text(project, "groupId").equals("atf.api")
                    && GovernanceDeployment.text(project, "version").equals(version), "GOV-BINARY-POM-COORDINATE");
            try (var archive = new JarFile(jar.toFile())) {
                var embedded = archive.getJarEntry("META-INF/maven/atf.api/atf-reporting/pom.xml");
                require(embedded != null, "GOV-BINARY-EMBEDDED-POM");
                require(java.util.Arrays.equals(archive.getInputStream(embedded).readAllBytes(), Files.readAllBytes(pom)), "GOV-BINARY-EMBEDDED-POM");
            }
        }
        assertStandalonePom();
    }
    public static ReportingGovernanceSurface read(Path root) throws Exception { return new ReportingGovernanceSurface(GovernanceDeployment.read(root)); }
    public boolean source() { return deployment.reportingSource(); }
    public String pom() throws Exception { return Files.readString(pom); }
    public void assertStandalonePom() throws Exception {
        var project = GovernanceDeployment.xml(pom());
        require(GovernanceDeployment.child(project, "parent") == null, "GOV-REPORTING-PARENT");
        require(GovernanceDeployment.children(GovernanceDeployment.child(project, "dependencies"), "dependency").stream()
                .noneMatch(dependency -> GovernanceDeployment.text(dependency, "groupId").equals("atf.api")), "GOV-REPORTING-ATF-DEPENDENCY");
    }
    public Set<String> entries() throws Exception {
        try (var archive = new JarFile(jar.toFile())) { return archive.stream().map(java.util.jar.JarEntry::getName).collect(Collectors.toUnmodifiableSet()); }
    }
    public boolean owns(Path logicalSource) throws Exception {
        Path base = deployment.root().resolve("atf-reporting/src/main/java");
        if (source() || !logicalSource.normalize().startsWith(base)) return Files.isRegularFile(logicalSource);
        String entry = base.relativize(logicalSource).toString().replace('\\', '/').replaceAll("\\.java$", ".class");
        return entries().contains(entry);
    }
    public Set<String> nativeTestManifest() throws Exception {
        var paths = new java.util.HashSet<String>();
        for (var row : producerInputs.path("nativeTests")) {
            paths.add(Path.of(row.path("path").asText()).getFileName().toString());
        }
        return Set.copyOf(paths);
    }
    public void assertBinaryLayout() throws Exception {
        var entries = entries();
        require(entries.contains("atf/reporting/contract/AtfReportPorts.class"), "GOV-BINARY-API");
        require(entries.stream().noneMatch(n -> n.endsWith(".java") || n.startsWith("atf/atf/") || n.startsWith("atf/reporting/standalone/")), "GOV-BINARY-LAYOUT");
        Path loaded = Path.of(ReportingSettings.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        require(Files.isRegularFile(loaded) && hash(loaded).equals(hash(jar)), "GOV-BINARY-CODESOURCE");
    }
    public List<String> forbiddenReferences(List<String> namespaces) throws Exception {
        var output = new java.io.StringWriter(); var errors = new java.io.StringWriter();
        var tool = ToolProvider.findFirst("jdeps").orElseThrow(() -> new IllegalStateException("GOV-BINARY-JDK: jdeps unavailable"));
        int exit = tool.run(new java.io.PrintWriter(output), new java.io.PrintWriter(errors),
                "--multi-release", "base", "--ignore-missing-deps", "-verbose:class", "-filter:none", jar.toString());
        require(exit == 0, "GOV-BINARY-JDEPS: " + errors);
        return output.toString().lines().filter(line -> namespaces.stream()
                .anyMatch(namespace -> line.matches(".*->\\s+" + java.util.regex.Pattern.quote(namespace) + "[.$].*"))).toList();
    }
    public static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
    static String hashJson(JsonNode node) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(node.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
    private static void require(boolean condition, String reason) { if (!condition) throw new IllegalStateException(reason); }
}
