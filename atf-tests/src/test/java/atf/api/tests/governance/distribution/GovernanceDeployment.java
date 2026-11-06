package atf.api.tests.governance.distribution;

import atf.core.config.module.AtfModuleIds;
import atf.core.files.AtfJavaSourceReferences;
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
        String coreVersion = text(child(pom, "properties"), "atf.core.version");
        boolean binaryCore = !coreVersion.isBlank();
        if (binaryCore && (source || !coreVersion.matches("[A-Za-z0-9][A-Za-z0-9._-]*")))
            throw new IllegalStateException("GOV-CORE-VERSION: invalid Core deployment");
        if (binaryCore && Files.exists(root.resolve(AtfModuleIds.CORE)))
            throw new IllegalStateException("GOV-CORE-SHADOW: source in binary deployment");
        String profile = text(child(pom, "properties"), "atf.consumer.profile");
        if (!profile.isBlank() && (source || !List.of("journeys", "foundation", "eventuate").contains(profile)))
            throw new IllegalStateException("GOV-CONSUMER-PROFILE: " + profile);
        boolean platform = !text(child(pom, "properties"), "atf.console.version").isBlank();
        boolean mock = !text(child(pom, "properties"), "atf.mock.version").isBlank();
        if (platform != mock || platform && source) throw new IllegalStateException("GOV-PLATFORM-VERSION");
        if (platform) for (String artifact : List.of("console", "mock")) {
            atf.libraries.PlatformLibrarySurface.version(root, artifact);
            if (Files.exists(root.resolve("atf-" + artifact))) throw new IllegalStateException("GOV-PLATFORM-SHADOW: " + artifact);
        }
        boolean binaryKit = !atf.libraries.PlatformLibrarySurface.version(root, "test-kit").isBlank();
        if (binaryKit && (source || actual.contains(AtfModuleIds.TEST_KIT) || Files.exists(root.resolve(AtfModuleIds.TEST_KIT))))
            throw new IllegalStateException("GOV-KIT-SHADOW: source in binary deployment");
        boolean foundation = List.of("foundation", "eventuate").contains(profile);
        List<String> binaryGate = AtfModuleIds.REACTOR_MODULES.stream()
                .filter(module -> source || !module.equals(AtfModuleIds.REPORTING))
                .filter(module -> !binaryCore || !module.equals(AtfModuleIds.CORE))
                .filter(module -> !binaryKit || !module.equals(AtfModuleIds.TEST_KIT))
                .filter(module -> !platform || !List.of(AtfModuleIds.CONSOLE, AtfModuleIds.MOCK).contains(module))
                .filter(module -> !foundation || !consumerModule(module))
                .filter(module -> !profile.equals("eventuate") || !List.of(AtfModuleIds.UI, AtfModuleIds.DEBUG_UI).contains(module)).toList();
        if (profile.equals("eventuate")) {
            var expanded = new ArrayList<>(binaryGate);
            int index = expanded.indexOf(AtfModuleIds.TESTS);
            expanded.addAll(index, List.of("atf-setup", "atf-test-eventuate"));
            binaryGate = List.copyOf(expanded);
        }
        List<String> binaryConsumer = binaryGate.stream().filter(module -> !module.equals(AtfModuleIds.REPORTING_TESTS)).toList();
        List<String> expected = source || actual.equals(binaryGate) ? binaryGate : binaryConsumer;
        if (!actual.equals(expected)) throw new IllegalStateException("GOV-REACTOR: expected=" + expected + "; observed=" + actual);
        for (String module : expected) if (!Files.isRegularFile(root.resolve(module).resolve("pom.xml")))
            throw new IllegalStateException("GOV-MODULE-POM: " + module);
        if (foundation) for (String module : List.of(AtfModuleIds.PRACTICE_SHOP, AtfModuleIds.AUTOMATION_EXERCISE))
            if (Files.exists(root.resolve(module))) throw new IllegalStateException("GOV-CONSUMER-SHADOW: " + module);
        if (profile.equals("eventuate")) for (String module : List.of(AtfModuleIds.UI, AtfModuleIds.DEBUG_UI))
            if (Files.exists(root.resolve(module))) throw new IllegalStateException("GOV-UI-SHADOW: " + module);
        if (foundation) assertNoConsumerSource(root, expected);
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

    public boolean coreSource() { return sourceModules.contains(AtfModuleIds.CORE); }

    public String coreVersion() throws Exception {
        return text(child(xml(Files.readString(root.resolve("pom.xml"))), "properties"), "atf.core.version");
    }

    public List<String> documentationTargets(String configured) {
        for (String artifact : List.of("console", "mock"))
            if (!sourceModules.contains("atf-" + artifact) && configured.equals("atf-" + artifact + "/README.md"))
                return List.of("docs/ATF_Platform_Libraries.md");
        if (!sourceModules.contains(AtfModuleIds.TEST_KIT) && configured.equals("atf-test-kit/README.md")) return List.of("lib/README.TestKit.md");
        if (!coreSource() && configured.equals("atf-core/README.md")) return List.of("docs/ATF_Core_Consumer.md");
        String module = configured.contains("/") ? configured.substring(0, configured.indexOf('/')) : "";
        if (List.of(AtfModuleIds.UI, AtfModuleIds.DEBUG_UI).contains(module) && !sourceModules.contains(module) && configured.equals(module + "/README.md"))
            return List.of("docs/ATF_J2_Verification.md", "docs/ATF_Eventuate_Integration.md");
        if (consumerModule(module) && !sourceModules.contains(module) && configured.equals(module + "/README.md"))
            return List.of("docs/ATF_Reporting_Consumer.md", "docs/ATF_Workspace_Synchronization.md");
        if (!sourceModules.contains(AtfModuleIds.REPORTING_TESTS)
                && List.of("atf-reporting-tests/README.md", "atf-reporting-tests/src/test/java/atf/ui/tests/extent/README.md").contains(configured))
            return List.of("docs/ATF_Reporting_Library_Atlas.md", "docs/ATF_Testing_Guide.md");
        return !reportingSource && configured.equals("atf-reporting/README.md")
                ? List.of("docs/ATF_Reporting_Library_Atlas.md", "docs/ATF_Reporting_Consumer.md") : List.of(configured);
    }

    private static boolean consumerModule(String module) {
        return List.of(AtfModuleIds.PRACTICE_SHOP, AtfModuleIds.AUTOMATION_EXERCISE).contains(module);
    }

    private static void assertNoConsumerSource(Path root, List<String> modules) throws Exception {
        for (String module : modules) {
            Path source = root.resolve(module).resolve("src");
            if (!Files.isDirectory(source)) continue;
            try (var files = Files.walk(source)) {
                for (Path file : files.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".java")).toList()) {
                    String code = Files.readString(file);
                    if (AtfJavaSourceReferences.containsNamespace(code, "atf.shop") || AtfJavaSourceReferences.containsNamespace(code, "atf.autoex"))
                        throw new IllegalStateException("GOV-CONSUMER-NAMESPACE: " + module);
                }
            }
        }
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
