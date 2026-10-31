package atf.api.tests.governance.distribution;

import atf.core.config.module.AtfModuleIds;
import org.w3c.dom.Element;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Explicit Maven source/binary perimeter; a missing directory never opts a guard out. */
public record GovernanceDeployment(Path root, List<String> sourceModules, boolean reportingSource) {
    public GovernanceDeployment { root = root.toAbsolutePath().normalize(); sourceModules = List.copyOf(sourceModules); }

    public static GovernanceDeployment read(Path root) throws Exception {
        Element pom = xml(Files.readString(root.resolve("pom.xml")));
        List<String> actual = children(child(pom, "modules"), "module").stream().map(Element::getTextContent).map(String::strip).toList();
        boolean source = actual.contains(AtfModuleIds.REPORTING);
        List<String> binaryGate = AtfModuleIds.REACTOR_MODULES.stream()
                .filter(module -> source || !module.equals(AtfModuleIds.REPORTING)).toList();
        List<String> binaryConsumer = binaryGate.stream().filter(module -> !module.equals(AtfModuleIds.REPORTING_TESTS)).toList();
        List<String> expected = source || actual.equals(binaryGate) ? binaryGate : binaryConsumer;
        if (!actual.equals(expected)) throw new IllegalStateException("GOV-REACTOR: expected=" + expected + "; observed=" + actual);
        for (String module : expected) if (!Files.isRegularFile(root.resolve(module).resolve("pom.xml")))
            throw new IllegalStateException("GOV-MODULE-POM: " + module);
        var deployment = new GovernanceDeployment(root, actual, source);
        if (!source) {
            if (Files.exists(root.resolve(AtfModuleIds.REPORTING))) throw new IllegalStateException("GOV-SHADOW: Reporting source in binary reactor");
            if (!actual.contains(AtfModuleIds.REPORTING_TESTS) && Files.exists(root.resolve(AtfModuleIds.REPORTING_TESTS)))
                throw new IllegalStateException("GOV-SHADOW: Reporting harness outside the declared reactor");
            if (text(child(pom, "properties"), "atf.reporting.version").isBlank())
                throw new IllegalStateException("GOV-BINARY-VERSION: absent Reporting coordinate");
        }
        return deployment;
    }

    public List<String> documentationTargets(String configured) {
        if (!sourceModules.contains(AtfModuleIds.REPORTING_TESTS)
                && List.of("atf-reporting-tests/README.md", "atf-reporting-tests/src/test/java/atf/ui/tests/extent/README.md").contains(configured))
            return List.of("docs/ATF_Reporting_Library_Atlas.md", "docs/ATF_Testing_Guide.md");
        return !reportingSource && configured.equals("atf-reporting/README.md")
                ? List.of("docs/ATF_Reporting_Library_Atlas.md", "docs/ATF_J1_Consumer.md", "lib/README.md") : List.of(configured);
    }

    public String reportingVersion() throws Exception {
        return text(child(xml(Files.readString(root.resolve("pom.xml"))), "properties"), "atf.reporting.version");
    }

    public static Element xml(String source) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(source.getBytes(java.nio.charset.StandardCharsets.UTF_8))).getDocumentElement();
    }
    public static List<Element> children(Element parent, String name) {
        var result = new ArrayList<Element>();
        if (parent != null) for (var node = parent.getFirstChild(); node != null; node = node.getNextSibling())
            if (node instanceof Element element && element.getTagName().equals(name)) result.add(element);
        return List.copyOf(result);
    }
    public static Element child(Element parent, String name) { return children(parent, name).stream().findFirst().orElse(null); }
    public static String text(Element parent, String name) { var value = child(parent, name); return value == null ? "" : value.getTextContent().strip(); }
}
