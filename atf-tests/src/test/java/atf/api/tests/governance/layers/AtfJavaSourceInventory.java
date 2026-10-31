package atf.api.tests.governance.layers;

import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Shared literal Maven source-root inventory for architecture and readability governance. */
public final class AtfJavaSourceInventory {
    private static final Set<String> EXCLUDED = Set.of(".git", ".tmp", "artifacts", "target");

    private AtfJavaSourceInventory() { }

    public static List<Path> mainSources(Path project) throws Exception {
        return sources(project, "sourceDirectory", "src/main/java");
    }

    public static List<Path> testSources(Path project) throws Exception {
        return sources(project, "testSourceDirectory", "src/test/java");
    }

    public static List<Path> allSources(Path project) throws Exception {
        Set<Path> result = new LinkedHashSet<>(mainSources(project));
        result.addAll(testSources(project));
        return result.stream().sorted().toList();
    }

    private static List<Path> sources(Path project, String element, String defaultDirectory) throws Exception {
        Path root = project.toAbsolutePath().normalize();
        Set<Path> sourceRoots = new LinkedHashSet<>();
        for (Path pom : pomFiles(root)) {
            Path module = pom.getParent();
            String configured = configuredDirectory(pom, element, defaultDirectory);
            Path source = module.resolve(configured).normalize();
            if (!source.startsWith(module) || source.equals(module)
                    || configured.contains("${")) {
                throw new IOException("Invalid " + element + " in " + pom + ": " + configured);
            }
            if (!Files.exists(source)) continue;
            if (!Files.isDirectory(source) || !source.toRealPath().startsWith(module.toRealPath())) {
                throw new IOException("Unsafe " + element + " in " + pom + ": " + configured);
            }
            sourceRoots.add(source);
        }
        List<Path> result = new ArrayList<>();
        for (Path source : sourceRoots) {
            try (var files = Files.walk(source)) {
                result.addAll(files.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .toList());
            }
        }
        return result.stream().distinct().sorted().toList();
    }

    private static String configuredDirectory(Path pom, String name, String defaultDirectory) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Element project = factory.newDocumentBuilder().parse(pom.toFile()).getDocumentElement();
        Element build = child(project, "build");
        Element configured = build == null ? null : child(build, name);
        if (configured == null) return defaultDirectory;
        String directory = configured.getTextContent().strip();
        if (directory.isBlank()) throw new IOException("Empty " + name + " in " + pom);
        return directory;
    }

    private static Element child(Element parent, String name) {
        var children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child && child.getTagName().equals(name)) return child;
        }
        return null;
    }

    private static List<Path> pomFiles(Path root) throws IOException {
        var result = new ArrayList<Path>();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                return !directory.equals(root) && EXCLUDED.contains(directory.getFileName().toString())
                        ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                if (attributes.isRegularFile() && file.getFileName().toString().equals("pom.xml")) result.add(file);
                return FileVisitResult.CONTINUE;
            }
        });
        return result.stream().sorted().toList();
    }
}
