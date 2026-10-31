package atf.api.tests.governance.documentation.factual;

import atf.core.config.module.AtfModuleIds;
import org.w3c.dom.Element;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Deliberately narrow architecture claims, compared with direct Maven declarations rather than another inventory. */
final class DocumentedBuildContracts {
    private static final Pattern MODULES = Pattern.compile("Reactorul păstrează exact cele (\\d+) module existente\\.");
    private static final Map<String, String> CONSUMERS = Map.of(
            "Shop", AtfModuleIds.PRACTICE_SHOP, "AutoEx", AtfModuleIds.AUTOMATION_EXERCISE);
    private DocumentedBuildContracts() { }

    static List<String> inspect(Path root, String document) throws Exception {
        var failures = new ArrayList<String>();
        var count = MODULES.matcher(document);
        int actual = children(child(pom(root.resolve("pom.xml")), "modules"), "module").size();
        if (!count.find()) failures.add(diagnostic(1, "reactor modules", "missing declaration", Integer.toString(actual), "pom.xml"));
        else {
            if (!Integer.toString(actual).equals(count.group(1))) failures.add(diagnostic(line(document, count.start()),
                    "reactor modules", count.group(1), Integer.toString(actual), "pom.xml"));
            if (count.find()) failures.add(diagnostic(line(document, count.start()), "reactor modules", "duplicate declaration", "one declaration", "pom.xml"));
        }
        for (var consumer : CONSUMERS.entrySet()) inspectConsumer(root, document, consumer.getKey(), consumer.getValue(), failures);
        return List.copyOf(failures);
    }

    private static void inspectConsumer(Path root, String document, String label, String module, List<String> failures) throws Exception {
        String source = module + "/pom.xml";
        boolean present = children(child(pom(root.resolve("pom.xml")), "modules"), "module").stream()
                .anyMatch(entry -> entry.getTextContent().strip().equals(module));
        String actual = present ? dependencyScope(pom(root.resolve(source)), AtfModuleIds.TEST_KIT) : "absent";
        List<String> rows = document.lines()
                .dropWhile(value -> !value.equals("| Consumer | `compile` | `test` |"))
                .skip(1).takeWhile(value -> value.startsWith("|"))
                .filter(value -> value.startsWith("| " + label + " |")).toList();
        String declared = "missing declaration";
        int line = 1;
        if (rows.size() == 1) {
            String row = rows.getFirst(); line = line(document, document.indexOf(row));
            String[] cells = row.split("\\|", -1);
            if (cells.length == 5) {
                boolean compile = tokens(cells[2]).contains("Test Kit"), test = tokens(cells[3]).contains("Test Kit");
                declared = cells[2].strip().equals("absent") && cells[3].strip().equals("absent") ? "absent"
                        : compile == test ? "missing or ambiguous scope" : compile ? "compile" : "test";
            }
        } else if (rows.size() > 1) declared = "duplicate declaration";
        if (!actual.equals(declared)) failures.add(diagnostic(line, label + " Test Kit scope", declared, actual, source));
    }

    private static List<String> tokens(String cell) { return Pattern.compile(",").splitAsStream(cell).map(String::strip).toList(); }
    private static String dependencyScope(Element project, String artifact) {
        for (var dependency : children(child(project, "dependencies"), "dependency")) {
            Element id = child(dependency, "artifactId");
            if (id != null && artifact.equals(id.getTextContent().strip())) {
                Element scope = child(dependency, "scope");
                return scope == null ? "compile" : scope.getTextContent().strip();
            }
        }
        return "absent";
    }
    private static Element pom(Path path) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(path.toFile()).getDocumentElement();
    }
    private static Element child(Element parent, String name) {
        var matches = children(parent, name); return matches.isEmpty() ? null : matches.getFirst();
    }
    private static List<Element> children(Element parent, String name) {
        var result = new ArrayList<Element>();
        if (parent != null) for (var node = parent.getFirstChild(); node != null; node = node.getNextSibling())
            if (node instanceof Element element && name.equals(element.getTagName())) result.add(element);
        return result;
    }
    private static int line(String document, int offset) { return (int) document.substring(0, offset).chars().filter(value -> value == '\n').count() + 1; }
    private static String diagnostic(int line, String claim, String declared, String actual, String source) {
        return "DOC-FACT docs/ATF_Architecture.md:" + line + " " + claim + ": declared=" + declared + "; observed=" + actual + "; source=" + source;
    }
}
