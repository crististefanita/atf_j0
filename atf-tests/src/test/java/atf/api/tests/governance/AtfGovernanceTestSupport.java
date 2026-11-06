package atf.api.tests.governance;

import atf.core.config.module.AtfModuleIds;
import atf.api.tests.governance.distribution.ReportingGovernanceSurface;
import atf.api.tests.governance.distribution.core.CoreGovernanceSurface;
import atf.api.tests.governance.distribution.GovernanceDeployment;
import atf.core.files.AtfFileTree;
import atf.core.files.AtfJavaSourceInventory;
import atf.core.files.AtfJavaSourceReferences;
import atf.core.files.AtfPathLocator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Shared filesystem and Maven assertions for the whole-ATF governance suite. */
public final class AtfGovernanceTestSupport {
    private static final String JAVA_SUFFIX = ".java";
    static final int PACKAGE_COHESION_LIMIT = 8;
    static final List<String> PRODUCTION_MODULES = AtfModuleIds.SOURCE_GOVERNED_MODULES;
    /** Reviewed exceptions are explicit seams, never a blanket package-size waiver. */
    static final Map<String, String> REVIEWED_COHESION_EXCEPTIONS = Map.of(
            "atf.core.http.response",
            "core HTTP response vocabulary and typed operation descriptors share one response boundary",
            "atf.reporting.api",
            "standalone reporting contracts, evidence adapters and JSON policy share the public reporting boundary",
            "atf.reporting.runtime.core",
            "runtime core fragments share the single generated-script publication boundary",
            "atf.reporting.runtime.section",
            "Extent runtime section facades and their typed JS fragments share one publication boundary",
            "atf.reporting.evidence",
            "typed evidence table normalization and HTML evidence rendering share one stable reporting boundary",
            "atf.reporting.html",
            "HTML shell, semantic snapshot, overview, tests and fails views share one artifact projection boundary");
    /** The whole-reactor suite is intentionally kept together as one readable gate. */
    static final Map<String, String> REVIEWED_TEST_COHESION_EXCEPTIONS = Map.of();
    private AtfGovernanceTestSupport() {
    }

    /** An omitted application is valid only in the explicitly declared foundation perimeter. */
    public static boolean consumerDeclared(Path root, String module) {
        if (!List.of(AtfModuleIds.PRACTICE_SHOP, AtfModuleIds.AUTOMATION_EXERCISE).contains(module))
            throw new IllegalArgumentException("Not an application consumer: " + module);
        try {
            boolean declared = GovernanceDeployment.read(root).sourceModules().contains(module);
            if (!declared) assertFalse(Files.exists(root.resolve(module)), "An excluded consumer cannot retain shadow source");
            return declared;
        } catch (Exception invalid) { throw new AssertionError("Invalid declared consumer perimeter", invalid); }
    }

    public static Path locateProjectRoot() {
        Path current = Path.of(System.getProperty("user.dir", "."))
                .toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isDirectory(current.resolve(AtfModuleIds.TESTS))
                    && Files.isDirectory(current.resolve(AtfModuleIds.VALIDATION))
                    && Files.isRegularFile(current.resolve("pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        return Path.of(System.getProperty("user.dir", "."))
                .toAbsolutePath().normalize();
    }

    static Path moduleSource(Path root, String module, boolean tests) {
        return sourceRoots(root, module, tests).getFirst();
    }

    static boolean containsJavaFileNamed(Path root, String suffix) throws Exception {
        return AtfFileTree.containsRegularFileEndingWith(root, suffix);
    }

    static void assertNoPackagePrefix(Path sourceRoot, String forbiddenPrefix)
            throws Exception {
        if (!Files.isDirectory(sourceRoot)) return;
        try (var files = Files.walk(sourceRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(JAVA_SUFFIX)).toList()) {
                String source = Files.readString(file);
                assertFalse(AtfJavaSourceReferences.containsNamespace(source, forbiddenPrefix),
                        () -> file + " contains forbidden Java namespace " + forbiddenPrefix);
            }
        }
    }

    static Map<String, Integer> productionPackageCounts(Path root) throws Exception {
        Map<String, Integer> counts = new TreeMap<>();
        for (String module : AtfModuleIds.SOURCE_GOVERNED_MODULES) {
            mergePackageCounts(AtfPathLocator.resolveModuleSourceRoot(
                    root, module, false), counts);
        }
        return counts;
    }

    static Map<String, Integer> testPackageCounts(Path sourceRoot) throws Exception {
        Map<String, Integer> counts = new TreeMap<>();
        mergePackageCounts(sourceRoot, counts);
        return counts;
    }

    static Map<String, List<String>> packageClassInventory(Path sourceRoot) throws Exception {
        Map<String, List<String>> inventory = new TreeMap<>();
        mergePackageClassInventory(sourceRoot, inventory);
        inventory.values().forEach(list -> list.sort(String::compareTo));
        return inventory;
    }

    static Map<String, List<String>> productionPackageClassInventory(Path root) throws Exception {
        Map<String, List<String>> inventory = new TreeMap<>();
        for (String module : AtfModuleIds.SOURCE_GOVERNED_MODULES) {
            mergePackageClassInventory(AtfPathLocator.resolveModuleSourceRoot(
                    root, module, false), inventory);
        }
        inventory.values().forEach(list -> list.sort(String::compareTo));
        return inventory;
    }

    private static void mergePackageClassInventory(Path sourceRoot,
                                                    Map<String, List<String>> inventory) throws Exception {
        if (!Files.isDirectory(sourceRoot)) return;
        AtfJavaSourceInventory.packages(sourceRoot).forEach((packageName, files) ->
                files.forEach(file -> inventory.computeIfAbsent(
                                packageName, ignored -> new ArrayList<>())
                        .add(file.getFileName().toString())));
    }

    private static void mergePackageCounts(Path sourceRoot,
                                           Map<String, Integer> counts) throws Exception {
        if (!Files.isDirectory(sourceRoot)) return;
        AtfJavaSourceInventory.packages(sourceRoot).forEach((packageName, files) ->
                counts.merge(packageName, files.size(), Integer::sum));
    }

    /** Resolves both conventional Maven roots and the legacy atf-tests layout. */
    static List<Path> sourceRoots(Path root, String module, boolean tests) {
        return List.of(AtfPathLocator.resolveModuleSourceRoot(root, module, tests));
    }

    static void assertDependencyPresent(Path root, String module, String dependency)
            throws Exception {
        String pom = modulePom(root, module);
        assertTrue(pom.contains("<artifactId>" + dependency + "</artifactId>"),
                () -> module + " must depend on " + dependency);
    }

    static void assertDependencyAbsent(Path root, String module, String dependency)
            throws Exception {
        String pom = modulePom(root, module);
        assertFalse(pom.contains("<artifactId>" + dependency + "</artifactId>"),
                () -> module + " must not depend on " + dependency);
    }

    static void assertProductionDependencyAbsent(Path root, String module, String dependency)
            throws Exception {
        String pom = modulePom(root, module);
        String marker = "<artifactId>" + dependency + "</artifactId>";
        int start = pom.indexOf(marker);
        if (start < 0) return;
        int end = pom.indexOf("</dependency>", start);
        assertTrue(end >= 0, () -> module + " has an unterminated dependency block for " + dependency);
        String block = pom.substring(start, end);
        assertTrue(block.contains("<scope>test</scope>"),
                () -> module + " must not have a production dependency on " + dependency);
    }

    static void assertDependencyScope(Path root, String module, String dependency, String scope)
            throws Exception {
        String pom = modulePom(root, module);
        String marker = "<artifactId>" + dependency + "</artifactId>";
        int dependencyStart = pom.indexOf(marker);
        assertTrue(dependencyStart >= 0,
                () -> module + " must declare dependency " + dependency);
        int dependencyEnd = pom.indexOf("</dependency>", dependencyStart);
        assertTrue(dependencyEnd >= 0,
                () -> module + " has an unterminated dependency block for " + dependency);
        String block = pom.substring(dependencyStart, dependencyEnd);
        assertTrue(block.contains("<scope>" + scope + "</scope>"),
                () -> module + " -> " + dependency + " must use " + scope + " scope");
    }

    static void assertAllAtfDependenciesAreTestScoped(Path root, String module)
            throws Exception {
        String pom = modulePom(root, module);
        var violations = new java.util.ArrayList<String>();
        int cursor = 0;
        while ((cursor = pom.indexOf("<dependency>", cursor)) >= 0) {
            int end = pom.indexOf("</dependency>", cursor);
            if (end < 0) break;
            String block = pom.substring(cursor, end);
            if (block.contains("<groupId>atf.api</groupId>")) {
                String artifact = extractXmlValue(block, "artifactId");
                if (artifact != null && !block.contains("<scope>test</scope>")) {
                    violations.add(artifact + " is not test-scoped");
                }
            }
            cursor = end + "</dependency>".length();
        }
        assertTrue(violations.isEmpty(), () -> module
                + " must keep every ATF dependency test-only: " + violations);
    }

    static void assertNoAtfDependencies(Path root, String module) throws Exception {
        String pom = modulePom(root, module);
        var violations = new java.util.ArrayList<String>();
        int cursor = 0;
        while ((cursor = pom.indexOf("<dependency>", cursor)) >= 0) {
            int end = pom.indexOf("</dependency>", cursor);
            if (end < 0) break;
            String block = pom.substring(cursor, end);
            if (block.contains("<groupId>atf.api</groupId>")) {
                String artifact = extractXmlValue(block, "artifactId");
                violations.add(artifact == null ? "unknown ATF dependency" : artifact);
            }
            cursor = end + "</dependency>".length();
        }
        assertTrue(violations.isEmpty(), () -> module
                + " must not declare ATF module dependencies: " + violations);
    }

    private static String modulePom(Path root, String module) throws Exception {
        return module.equals(AtfModuleIds.REPORTING) ? ReportingGovernanceSurface.read(root).pom()
                : module.equals(AtfModuleIds.CORE) ? CoreGovernanceSurface.read(root).pom()
                : List.of(AtfModuleIds.CONSOLE, AtfModuleIds.MOCK, AtfModuleIds.TEST_KIT).contains(module)
                ? new atf.libraries.PlatformLibrarySurface(root, module.substring("atf-".length())).pom()
                : Files.readString(root.resolve(module).resolve("pom.xml"));
    }

    private static String extractXmlValue(String block, String element) {
        String open = "<" + element + ">";
        String close = "</" + element + ">";
        int start = block.indexOf(open);
        if (start < 0) return null;
        start += open.length();
        int end = block.indexOf(close, start);
        return end < 0 ? null : block.substring(start, end).trim();
    }

    static void assertNoForbidden(Path sourceRoot, List<String> forbidden,
                                  List<String> violations) throws Exception {
        if (!Files.isDirectory(sourceRoot)) {
            violations.add("missing source root: " + sourceRoot);
            return;
        }
        try (var files = Files.walk(sourceRoot)) {
            files.filter(path -> path.toString().endsWith(JAVA_SUFFIX)).forEach(path -> {
                try {
                    String source = Files.readString(path);
                    forbidden.stream().filter(token -> containsForbiddenToken(source, token)).forEach(token ->
                            violations.add(path + " contains " + token));
                } catch (Exception error) {
                    violations.add("cannot read " + path + ": " + error.getMessage());
                }
            });
        }
    }

    static void assertNoForbiddenReportingCore(Path sourceRoot, List<String> forbidden,
                                                List<String> violations) throws Exception {
        if (!Files.isDirectory(sourceRoot)) {
            violations.add("missing source root: " + sourceRoot);
            return;
        }
        Path acceptanceAdapter = sourceRoot.resolve("atf/reporting/api/acceptance");
        try (var files = Files.walk(sourceRoot)) {
            files.filter(path -> path.toString().endsWith(JAVA_SUFFIX))
                    .filter(path -> !path.startsWith(acceptanceAdapter))
                    .forEach(path -> {
                        try {
                            String source = Files.readString(path);
                            forbidden.stream().filter(token -> containsForbiddenToken(source, token)).forEach(token ->
                                    violations.add(path + " contains " + token));
                        } catch (Exception error) {
                            violations.add("cannot read " + path + ": " + error.getMessage());
                        }
                    });
        }
    }

    private static boolean containsForbiddenToken(String source, String token) {
        if (token.contains(".") && !token.startsWith("<")) {
            return AtfJavaSourceReferences.containsNamespace(source, token);
        }
        return source.contains(token);
    }
}
