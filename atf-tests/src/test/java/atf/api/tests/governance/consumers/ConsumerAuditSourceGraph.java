package atf.api.tests.governance.consumers;

import atf.api.tests.governance.layers.AtfTestLayerArchitectureTest;
import atf.core.files.AtfJavaSourceReferences;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Conservative source reachability; execution/report probes remain the runtime proof. */
final class ConsumerAuditSourceGraph {
    private static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern IMPORT = Pattern.compile("(?m)^\\s*import\\s+(?:static\\s+)?([\\w.]+?)(\\.\\*)?\\s*;");
    private static final Pattern TYPE = Pattern.compile("\\b(?:class|interface|enum|record)\\s+\\w+");
    private static final Pattern PARENT = Pattern.compile("\\bclass\\s+\\w+(?:\\s*<[^>]+>)?\\s+extends\\s+([\\w.]+)");
    private static final Set<String> MARKERS = Set.of("@AutoExComponentTest", "@ShopComponentTest");

    static List<String> violations(Map<String, String> rawSources, Map<String, String> rawAnnotations) {
        var sources = codeSources(rawSources);
        var annotations = codeSources(rawAnnotations);
        var violations = new ArrayList<String>();
        var references = new java.util.HashMap<String, List<String>>();
        for (var entry : sources.entrySet()) {
            String source = entry.getValue();
            var declaration = TYPE.matcher(source);
            if (!declaration.find()) continue;
            String header = source.substring(0, declaration.end());
            if (header.contains("@interface") || header.matches("(?s).*\\babstract\\b.*")
                    || !AtfTestLayerArchitectureTest.declaredAnnotations(source, annotations, Set.of("@Suite")).isEmpty()) continue;
            var hierarchy = hierarchy(entry.getKey(), sources, new HashSet<>());
            boolean component = hierarchy.stream().anyMatch(type -> AtfTestLayerArchitectureTest
                    .declaredLayers(sources.get(type), annotations).contains("@AtfComponentTest"));
            if (!component) continue;
            boolean reporting = hierarchy.stream().anyMatch(type -> !AtfTestLayerArchitectureTest
                    .declaredAnnotations(sources.get(type), annotations, MARKERS).isEmpty());
            if (!reporting) violations.add(entry.getKey() + " needs its consumer reporting marker");
            var visited = new HashSet<String>();
            for (String type : hierarchy) inspect(type, sources, references, visited, violations, entry.getKey());
        }
        return List.copyOf(violations);
    }

    private static Map<String, String> codeSources(Map<String, String> sources) {
        var code = new java.util.HashMap<String, String>();
        sources.forEach((name, source) -> code.put(name, AtfJavaSourceReferences.codeOnly(source)));
        return Map.copyOf(code);
    }

    private static List<String> hierarchy(String type, Map<String, String> sources, Set<String> seen) {
        if (!seen.add(type)) return List.of();
        var result = new ArrayList<String>();
        result.add(type);
        var parent = PARENT.matcher(sources.get(type));
        if (parent.find()) {
            String resolved = resolve(parent.group(1), sources.get(type), sources);
            if (resolved != null) result.addAll(hierarchy(resolved, sources, seen));
        }
        return result;
    }

    private static void inspect(String type, Map<String, String> sources, Map<String, List<String>> references, Set<String> seen,
                                 List<String> violations, String root) {
        if (!seen.add(type)) return;
        String source = sources.get(type);
        if (AtfJavaSourceReferences.containsNamespace(source, "org.junit.jupiter.api.Assertions"))
            violations.add(root + " reaches unaudited assertions in " + type);
        for (String candidate : references.computeIfAbsent(type, key -> references(key, sources)))
            inspect(candidate, sources, references, seen, violations, root);
    }

    private static List<String> references(String type, Map<String, String> sources) {
        String source = sources.get(type);
        var references = new ArrayList<String>();
        String executableReferences = source.replaceAll("(?m)^\\s*import[^;]+;", "");
        for (String candidate : sources.keySet()) {
            if (candidate.equals(type)) continue;
            String simple = candidate.substring(candidate.lastIndexOf('.') + 1);
            String withoutClassLiterals = executableReferences.replaceAll("\\b" + Pattern.quote(simple) + "\\s*\\.\\s*class\\b", "");
            boolean qualified = AtfJavaSourceReferences.containsNamespace(withoutClassLiterals, candidate);
            boolean simpleReference = AtfJavaSourceReferences.containsNamespace(withoutClassLiterals, simple)
                    && candidate.equals(resolve(simple, source, sources));
            boolean staticImport = Pattern.compile("\\bimport\\s+static\\s+" + Pattern.quote(candidate) + "\\s*\\.")
                    .matcher(source).find() && AtfJavaSourceReferences.containsNamespace(source, candidate);
            if (qualified || simpleReference || staticImport) references.add(candidate);
        }
        return List.copyOf(references);
    }

    private static String resolve(String name, String source, Map<String, String> sources) {
        if (sources.containsKey(name)) return name;
        for (var match : IMPORT.matcher(source).results().toList()) {
            String imported = match.group(1);
            if (imported.endsWith("." + name) && sources.containsKey(imported)) return imported;
            if (match.group(2) != null && sources.containsKey(imported + "." + name)) return imported + "." + name;
        }
        var owner = PACKAGE.matcher(source);
        String local = owner.find() ? owner.group(1) + "." + name : name;
        return sources.containsKey(local) ? local : null;
    }
}
