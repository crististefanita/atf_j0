package atf.libraries;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;

/** Consumer acceptance inspects the declared physical library; it never reconstructs its source. */
public record PlatformLibrarySurface(Path root, String artifact) {
    public PlatformLibrarySurface {
        root = root.toAbsolutePath().normalize();
        require(List.of("console", "mock", "test-kit").contains(artifact), "LIB-ARTIFACT");
    }

    private String namespace() { return artifact.equals("test-kit") ? "atf/testkit/" : "atf/" + artifact + "/"; }

    public static Path workspace() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml")) && Files.isDirectory(current.resolve("atf-tests"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("LIB-WORKSPACE");
    }

    public boolean source() throws Exception { return version(root, artifact).isBlank(); }
    public Path jar() throws Exception { return root.resolve("lib/atf-" + artifact + "-" + version(root, artifact) + ".jar"); }
    public String pom() throws Exception {
        Path file = source() ? root.resolve("atf-" + artifact + "/pom.xml")
                : root.resolve("lib/atf-" + artifact + "-" + version(root, artifact) + ".pom");
        return Files.readString(file);
    }

    public void inspect() throws Exception {
        var project = xml(pom());
        require(project.getElementsByTagName("parent").getLength() == 0, "LIB-PARENT");
        require(text(project, "groupId").equals("atf.api") && text(project, "artifactId").equals("atf-" + artifact), "LIB-COORDINATE");
        var dependencies = project.getElementsByTagName("dependency");
        for (int index = 0; index < dependencies.getLength(); index++) {
            var dependency = (Element) dependencies.item(index);
            require(!text(dependency, "version").isBlank() || text(dependency, "groupId").startsWith("org.junit"), "LIB-DEPENDENCY-VERSION");
            if (artifact.equals("test-kit")) require(!text(dependency, "groupId").equals("atf.api"), "LIB-KIT-BOUNDARY");
            if (!text(dependency, "groupId").equals("atf.api")) continue;
            String id = text(dependency, "artifactId");
            require(id.equals("atf-core") || id.equals("atf-test-kit"), "LIB-BOUNDARY");
            if (id.equals("atf-test-kit")) require(text(dependency, "scope").equals("test")
                    || artifact.equals("console") && text(dependency, "optional").equals("true"), "LIB-KIT-SCOPE");
        }
        if (source()) {
            require(Files.isDirectory(root.resolve("atf-" + artifact + "/src/main/java").resolve(namespace())), "LIB-SOURCE");
            return;
        }
        require(!Files.exists(root.resolve("atf-" + artifact)), "LIB-SOURCE-SHADOW");
        require(text(project, "version").equals(version(root, artifact)), "LIB-COORDINATE");
        if (!artifact.equals("test-kit")) require(text(project, "atf.core.version").equals(version(root, "core")), "LIB-CORE-VERSION");
        var ownership = new ObjectMapper().readTree(root.resolve("library-provenance.json").toFile()).path("platformLibraryOwnership");
        require(ownership.path("sourceOwner").asText().equals("atf_j0")
                && ownership.path("feedbackOwner").asText().equals("atf_j0")
                && ownership.path("feedbackRegistry").asText().equals("atf_j0/docs/feedback/README.md")
                && ownership.path("contract").asText().equals("lib/README.md"), "LIB-OWNERSHIP");
        var metadata = metadata();
        require(metadata.path("coordinate").asText().equals("atf.api:atf-" + artifact + ":" + version(root, artifact)), "LIB-PROVENANCE-COORDINATE");
        require(hash(jar()).equalsIgnoreCase(metadata.path("binarySha256").asText()), "LIB-JAR-HASH");
        Path publishedPom = jar().resolveSibling(jar().getFileName().toString().replaceAll("\\.jar$", ".pom"));
        require(hash(publishedPom).equalsIgnoreCase(metadata.path("publishedPomSha256").asText()), "LIB-POM-HASH");
        JsonNode inputs = metadata.path("producerInputs");
        require(digest(inputs.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .equalsIgnoreCase(metadata.path("producerInputsSha256").asText()), "LIB-INPUTS-HASH");
        require(inputs.path("files").asInt() > 0 && inputs.path("sha256").asText().matches("[a-fA-F0-9]{64}"), "LIB-INPUTS");
        var names = new java.util.HashSet<String>();
        require(inputs.path("nativeTests").isArray() && !inputs.path("nativeTests").isEmpty(), "LIB-NATIVE-CATALOG");
        for (var row : inputs.path("nativeTests")) {
            String name = row.path("path").asText();
            require(name.startsWith("src/test/java/") && name.endsWith(".java") && !name.contains("..") && !name.contains("\\")
                    && names.add(name) && row.path("sha256").asText().matches("[a-fA-F0-9]{64}"), "LIB-NATIVE-CATALOG");
        }
        try (var archive = new JarFile(jar().toFile())) {
            var embedded = archive.getJarEntry("META-INF/maven/atf.api/atf-" + artifact + "/pom.xml");
            require(embedded != null && java.util.Arrays.equals(archive.getInputStream(embedded).readAllBytes(), Files.readAllBytes(publishedPom)), "LIB-EMBEDDED-POM");
            require(archive.getJarEntry(namespace() + (artifact.equals("console") ? "AtfConsoleReporter" : artifact.equals("mock") ? "AtfMockTransport" : "assertions/RecordedAssertions") + ".class") != null, "LIB-API");
            require(archive.stream().noneMatch(entry -> entry.getName().endsWith(".java")
                    || entry.getName().endsWith(".class") && !entry.getName().startsWith(namespace())), "LIB-NAMESPACE");
        }
        assertFlatLayout(root);
    }

    public void assertLoaded(Class<?> entry) throws Exception {
        if (source()) return;
        Path loaded = Path.of(entry.getProtectionDomain().getCodeSource().getLocation().toURI());
        require(Files.isRegularFile(loaded) && loaded.getFileName().equals(jar().getFileName()) && hash(loaded).equals(hash(jar())), "LIB-CODESOURCE");
    }

    public boolean owns(Path logical) throws Exception {
        if (source()) return Files.isRegularFile(logical);
        inspect();
        String relative = root.resolve("atf-" + artifact).relativize(logical).toString().replace('\\', '/');
        if (relative.startsWith("src/test/")) {
            for (var row : metadata().path("producerInputs").path("nativeTests")) if (row.path("path").asText().equals(relative)) return true;
            return false;
        }
        if (!relative.startsWith("src/main/java/")) return false;
        try (var archive = new JarFile(jar().toFile())) {
            return archive.getJarEntry(relative.substring("src/main/java/".length()).replaceAll("\\.java$", ".class")) != null;
        }
    }

    public List<String> forbiddenReferences(List<String> namespaces) throws Exception {
        var output = new java.io.StringWriter(); var errors = new java.io.StringWriter();
        int exit = java.util.spi.ToolProvider.findFirst("jdeps").orElseThrow().run(new java.io.PrintWriter(output), new java.io.PrintWriter(errors),
                "--multi-release", "base", "--ignore-missing-deps", "-verbose:class", "-filter:none", jar().toString());
        require(exit == 0, "LIB-JDEPS: " + errors);
        return output.toString().lines().filter(line -> namespaces.stream()
                .anyMatch(namespace -> line.matches(".*->\\s+" + java.util.regex.Pattern.quote(namespace) + "[.$].*"))).toList();
    }

    public static void assertFlatLayout(Path root) throws Exception {
        var expected = new java.util.HashSet<String>();
        expected.addAll(List.of("README.md", "README.Core.md", "README.Reporting.md", "README.Console.md", "README.Mock.md", "README.TestKit.md"));
        for (String artifact : List.of("core", "reporting", "console", "mock", "test-kit")) {
            String version = version(root, artifact);
            if (!version.isBlank()) for (String extension : List.of("jar", "pom")) expected.add("atf-" + artifact + "-" + version + "." + extension);
        }
        try (var files = Files.list(root.resolve("lib"))) {
            var actual = files.toList();
            require(actual.stream().allMatch(Files::isRegularFile)
                    && actual.stream().map(file -> file.getFileName().toString()).collect(Collectors.toSet()).equals(expected), "LIB-FLAT-LAYOUT");
        }
    }

    public static String version(Path root, String artifact) throws Exception {
        String value = text(xml(Files.readString(root.resolve("pom.xml"))), "atf." + (artifact.equals("test-kit") ? "testkit" : artifact) + ".version");
        require(value.isBlank() || value.matches("[A-Za-z0-9][A-Za-z0-9._-]*"), "LIB-UNSAFE-VERSION");
        return value;
    }
    private JsonNode metadata() throws Exception {
        return new ObjectMapper().readTree(root.resolve("library-provenance.json").toFile()).path("platformLibraries").path(artifact);
    }
    public static String hash(Path path) throws Exception { return digest(Files.readAllBytes(path)); }
    private static String digest(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static Element xml(String source) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(source.getBytes(java.nio.charset.StandardCharsets.UTF_8))).getDocumentElement();
    }
    private static String text(Element parent, String tag) {
        var elements = parent.getElementsByTagName(tag);
        require(elements.getLength() <= 1 || !tag.startsWith("atf."), "LIB-DUPLICATE-VERSION");
        return elements.getLength() == 0 ? "" : elements.item(0).getTextContent().strip();
    }
    private static void require(boolean condition, String diagnostic) { if (!condition) throw new IllegalStateException(diagnostic); }
}
