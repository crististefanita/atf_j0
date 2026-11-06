package atf.testkit.documentation.guides;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.jar.JarFile;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;

/** Validates the selected flat binary publication without looking for a neighbouring producer. */
final class GuideBundle {
    private GuideBundle() { }

    static Path selected() {
        String value = System.getProperty("atf.guide.bundle", "").strip();
        if (value.isEmpty()) throw new IllegalStateException("Set -Datf.guide.bundle to the distributed flat lib directory");
        Path bundle = Path.of(value).toAbsolutePath().normalize();
        if (!Files.isDirectory(bundle)) throw new IllegalStateException("Missing binary bundle: " + bundle);
        return bundle;
    }

    static List<Artifact> publish(Path bundle, Path repository) throws Exception {
        var artifacts = new ArrayList<Artifact>();
        for (String id : List.of("core", "reporting", "console", "mock", "test-kit")) {
            List<Path> matches;
            try (var files = Files.list(bundle)) {
                matches = files.filter(file -> file.getFileName().toString().matches("atf-" + id + "-.+\\.pom")).toList();
            }
            if (matches.size() != 1) throw new IllegalStateException("Expected one published POM: " + id);
            Path pom = matches.getFirst();
            Element project = xml(pom);
            if (project.getElementsByTagName("parent").getLength() != 0) throw new IllegalStateException("Publication has an ATF parent");
            String group = direct(project, "groupId"), artifact = direct(project, "artifactId"), version = direct(project, "version");
            if (!group.equals("atf.api") || !artifact.equals("atf-" + id) || !version.matches("[A-Za-z0-9][A-Za-z0-9._-]*"))
                throw new IllegalStateException("Invalid explicit publication coordinates: " + pom);
            String name = artifact + "-" + version;
            Path jar = bundle.resolve(name + ".jar");
            if (!pom.getFileName().toString().equals(name + ".pom") || !Files.isRegularFile(jar))
                throw new IllegalStateException("Missing matching JAR/POM pair: " + name);
            try (var archive = new JarFile(jar.toFile())) {
                var metadata = archive.getJarEntry("META-INF/maven/" + group + "/" + artifact + "/pom.xml");
                if (metadata == null) throw new IllegalStateException("Missing embedded POM: " + jar);
                try (var input = archive.getInputStream(metadata)) {
                    if (!Arrays.equals(input.readAllBytes(), Files.readAllBytes(pom)))
                        throw new IllegalStateException("Adjacent and embedded POM differ: " + name);
                }
            }
            Path target = Files.createDirectories(repository.resolve("atf/api/" + artifact + "/" + version));
            Files.copy(jar, target.resolve(name + ".jar"));
            Files.copy(pom, target.resolve(name + ".pom"));
            artifacts.add(new Artifact(artifact, version, jar, hash(jar)));
        }
        return List.copyOf(artifacts);
    }

    static String hash(Path file) throws Exception {
        try (var input = Files.newInputStream(file)) {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            for (int count; (count = input.read(buffer)) != -1;) digest.update(buffer, 0, count);
            return HexFormat.of().formatHex(digest.digest());
        }
    }

    private static Element xml(Path file) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(file.toFile()).getDocumentElement();
    }

    private static String direct(Element parent, String name) {
        var values = new ArrayList<String>();
        for (var node = parent.getFirstChild(); node != null; node = node.getNextSibling())
            if (node instanceof Element element && element.getTagName().equals(name)) values.add(element.getTextContent().strip());
        if (values.size() != 1) throw new IllegalStateException("Expected one explicit " + name);
        return values.getFirst();
    }

    record Artifact(String id, String version, Path jar, String hash) { }
}
