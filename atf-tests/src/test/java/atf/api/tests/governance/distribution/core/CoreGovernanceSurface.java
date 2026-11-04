package atf.api.tests.governance.distribution.core;

import atf.api.tests.governance.distribution.GovernanceDeployment;
import atf.api.tests.governance.distribution.ReportingGovernanceSurface;
import atf.core.config.module.AtfModuleIds;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/** Core source remains producer-owned; consumers inspect the exact declared artifact. */
public final class CoreGovernanceSurface {
    private final GovernanceDeployment deployment;
    private final Path pom;
    private final Path jar;

    private CoreGovernanceSurface(GovernanceDeployment deployment) throws Exception {
        this.deployment = deployment;
        String version = deployment.coreSource() ? "" : deployment.coreVersion();
        pom = deployment.root().resolve(source() ? "atf-core/pom.xml" : "lib/atf-core-" + version + ".pom");
        jar = source() ? null : deployment.root().resolve("lib/atf-core-" + version + ".jar");
        if (!source()) {
            var metadata = new ObjectMapper().readTree(deployment.root().resolve("library-provenance.json").toFile()).path("coreLibrary");
            require(metadata.path("coordinate").asText().equals("atf.api:atf-core:" + version), "GOV-CORE-COORDINATE");
            require(ReportingGovernanceSurface.hash(jar).equalsIgnoreCase(metadata.path("binarySha256").asText()), "GOV-CORE-HASH");
            require(ReportingGovernanceSurface.hash(pom).equalsIgnoreCase(metadata.path("publishedPomSha256").asText()), "GOV-CORE-POM-HASH");
            var inputs = metadata.path("producerInputs");
            String digest = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(inputs.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            require(digest.equalsIgnoreCase(metadata.path("producerInputsSha256").asText()), "GOV-CORE-INPUTS-HASH");
            require(inputs.path("files").asInt() > 0 && inputs.path("sha256").asText().matches("[A-Fa-f0-9]{64}"), "GOV-CORE-INPUTS");
            var tests = inputs.path("nativeTests"); var names = new java.util.HashSet<String>();
            require(tests.isArray() && !tests.isEmpty(), "GOV-CORE-NATIVE-TESTS");
            for (var test : tests) {
                String name = test.path("path").asText();
                require(name.startsWith("src/test/java/") && name.endsWith(".java") && !name.contains("..")
                        && !name.contains("\\") && names.add(name) && test.path("sha256").asText().matches("[A-Fa-f0-9]{64}"), "GOV-CORE-NATIVE-TESTS");
            }
            var project = GovernanceDeployment.xml(pom());
            require(GovernanceDeployment.text(project, "groupId").equals("atf.api")
                    && GovernanceDeployment.text(project, "artifactId").equals("atf-core")
                    && GovernanceDeployment.text(project, "version").equals(version), "GOV-CORE-POM-COORDINATE");
            try (var archive = new JarFile(jar.toFile())) {
                var embedded = archive.getJarEntry("META-INF/maven/atf.api/atf-core/pom.xml");
                require(embedded != null && java.util.Arrays.equals(archive.getInputStream(embedded).readAllBytes(), Files.readAllBytes(pom)), "GOV-CORE-EMBEDDED-POM");
            }
            assertFlatLayout();
        }
        assertStandalonePom();
    }
    public static CoreGovernanceSurface read(Path root) throws Exception { return new CoreGovernanceSurface(GovernanceDeployment.read(root)); }
    public boolean source() { return deployment.coreSource(); }
    public String pom() throws Exception { return Files.readString(pom); }
    public void assertStandalonePom() throws Exception {
        var project = GovernanceDeployment.xml(pom());
        require(GovernanceDeployment.child(project, "parent") == null, "GOV-CORE-PARENT");
        for (var dependency : GovernanceDeployment.children(GovernanceDeployment.child(project, "dependencies"), "dependency")) {
            if (GovernanceDeployment.text(dependency, "scope").equals("test")) continue;
            String group = GovernanceDeployment.text(dependency, "groupId");
            require(!group.equals("atf.api") && !group.startsWith("org.junit") && !group.startsWith("com.aventstack"), "GOV-CORE-PRODUCTION-DEPENDENCY");
            require(!GovernanceDeployment.text(dependency, "version").isBlank(), "GOV-CORE-DEPENDENCY-VERSION");
        }
    }
    public boolean owns(Path logicalSource) throws Exception {
        Path base = deployment.root().resolve("atf-core/src/main/java");
        if (source() || !logicalSource.normalize().startsWith(base)) return Files.isRegularFile(logicalSource);
        return entries().contains(base.relativize(logicalSource).toString().replace('\\', '/').replaceAll("\\.java$", ".class"));
    }
    private Set<String> entries() throws Exception {
        try (var archive = new JarFile(jar.toFile())) { return archive.stream().map(java.util.jar.JarEntry::getName).collect(Collectors.toUnmodifiableSet()); }
    }
    public void assertBinaryLayout() throws Exception {
        var entries = entries();
        require(entries.contains("atf/core/config/module/AtfModuleIds.class"), "GOV-CORE-API");
        require(entries.stream().noneMatch(name -> name.endsWith(".java") || name.endsWith("Test.class")
                || name.endsWith(".class") && !name.startsWith("atf/core/")), "GOV-CORE-BINARY-LAYOUT");
        Path loaded = Path.of(AtfModuleIds.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        require(Files.isRegularFile(loaded) && ReportingGovernanceSurface.hash(loaded).equals(ReportingGovernanceSurface.hash(jar)), "GOV-CORE-CODESOURCE");
    }
    public List<String> forbiddenReferences(List<String> namespaces) throws Exception {
        var output = new java.io.StringWriter(); var errors = new java.io.StringWriter();
        var tool = java.util.spi.ToolProvider.findFirst("jdeps").orElseThrow();
        int exit = tool.run(new java.io.PrintWriter(output), new java.io.PrintWriter(errors),
                "--multi-release", "base", "--ignore-missing-deps", "-verbose:class", "-filter:none", jar.toString());
        require(exit == 0, "GOV-CORE-JDEPS: " + errors);
        return output.toString().lines().filter(line -> namespaces.stream()
                .anyMatch(namespace -> line.matches(".*->\\s+" + java.util.regex.Pattern.quote(namespace) + "[.$].*"))).toList();
    }
    private void assertFlatLayout() throws Exception {
        try { atf.libraries.PlatformLibrarySurface.assertFlatLayout(deployment.root()); }
        catch (IllegalStateException invalid) { throw new IllegalStateException("GOV-CORE-FLAT-LIB", invalid); }
    }
    private static void require(boolean condition, String reason) { if (!condition) throw new IllegalStateException(reason); }
}
