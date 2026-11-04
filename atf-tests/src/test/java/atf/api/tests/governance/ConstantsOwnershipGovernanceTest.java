package atf.api.tests.governance;

import atf.api.tests.governance.distribution.ReportingHarnessOwnership;

import atf.api.tests.governance.distribution.ReportingGovernanceSurface;
import atf.api.tests.governance.distribution.core.CoreGovernanceSurface;
import atf.testkit.AtfComponentTest;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.regex.Pattern;

import static atf.api.tests.governance.AtfGovernanceTestSupport.locateProjectRoot;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prevents canonical protocol identities from leaking back into audited tests. */
@AtfComponentTest
public class ConstantsOwnershipGovernanceTest {
    private static final List<String> REVIEWED_SOURCES = List.of(
            "atf-mock/src/main/java/atf/mock/AtfMockConfiguration.java",
            "atf-validation/src/main/java/atf/validation/contract/ContractValidators.java",
            "atf-validation/src/main/java/atf/validation/contract/ResponseContractRuleSet.java",
            "atf-mock/src/test/java/atf/mock/AtfMockTransportTest.java",
            "atf-mock/src/test/java/atf/mock/MockRouteIdentityTest.java",
            "atf-reporting-tests/src/test/java/atf/reporting/api/tests/api/flows/ApiFlowAssertions.java");
    private static final List<Pattern> FORBIDDEN_CANONICALS = List.of(
            Pattern.compile("(?s)new\\s+MockRoute\\([^;]*?\\\"/public/v2/"),
            Pattern.compile("new\\s+ApiRoute\\([^;]*?\\\"(?:GET|POST|PUT)\\\""),
            Pattern.compile("ApiRequest\\.of\\(\\\"(?:GET|POST|PUT)\\\""),
            Pattern.compile("(?s)(?:body|resourceId|compareResponse)\\([^;]*?\\\"(?:GET|POST|PUT)\\\""),
            Pattern.compile("contentTypeContains\\(\\\"application/json\\\"\\)"),
            Pattern.compile("header\\(\\\"Content-Type\\\"\\)"),
            Pattern.compile("Map\\.of\\(\\\"Content-Type\\\"\\s*,\\s*\\\"application/json\\\""),
            Pattern.compile("\\\"https://atf\\.local/"));
    private static final List<String> LITERAL_OWNER_EXEMPTIONS = List.of(
            "atf-tests/src/test/java/atf/api/tests/governance/ConstantsOwnershipGovernanceTest.java",
            "atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/architecture/DeclarativeUiTestArchitectureTest.java",
            "atf-reporting-tests/src/test/java/atf/reporting/support/ApiReportTestUrls.java");

    @Test
    void canonicalProtocolAndRouteValuesHaveSemanticOwners() throws Exception {
        Path root = locateProjectRoot();
        var violations = new ArrayList<String>();
        for (String relative : REVIEWED_SOURCES) {
            if (relative.startsWith("atf-reporting-tests/")) {
                ReportingHarnessOwnership.read(root).inspect(() -> inspectCanonicalSource(root, relative, violations));
            } else inspectCanonicalSource(root, relative, violations);
        }
        assertTrue(violations.isEmpty(), () -> "Constants ownership violations: " + violations);
    }

    @Test
    void coreKeepsPathMechanicsGenericAndLeavesConcreteCatalogsAtTheirBoundaries() throws Exception {
        Path root = locateProjectRoot();
        assertTrue(CoreGovernanceSurface.read(root).owns(root.resolve(
                        "atf-core/src/main/java/atf/core/routing/ApiPathTemplate.java")),
                "core must expose the generic path-template mechanism");
        assertTrue(!CoreGovernanceSurface.read(root).owns(root.resolve(
                        "atf-core/src/main/java/atf/core/routing/ApiPaths.java")),
                "core must not own a concrete product route catalog");
        ReportingHarnessOwnership.read(root).requireFile(root.resolve(
                "atf-reporting-tests/src/test/java/atf/reporting/api/execution/routing/RoutePaths.java"));
        assertTrue(new atf.libraries.PlatformLibrarySurface(root, "mock").owns(root.resolve(
                        "atf-mock/src/main/java/atf/mock/MockRoutePaths.java")));
    }

    @Test
    void reusableProtocolDomAuthAndDiagnosticVocabularyStayWithPlatformOwners() throws Exception {
        Path root = locateProjectRoot();
        List<String> requiredOwners = List.of(
                "atf-core/src/main/java/atf/core/http/protocol/ApiUris.java",
                "atf-core/src/main/java/atf/core/http/protocol/HttpHeaderProfiles.java",
                "atf-core/src/main/java/atf/core/http/auth/AuthProfiles.java",
                "atf-core/src/main/java/atf/core/diagnostics/ThrowableDiagnostics.java",
                "atf-ui/src/main/java/atf/ui/dom/HtmlElement.java",
                "atf-ui/src/main/java/atf/ui/dom/HtmlAttribute.java",
                "atf-ui/src/main/java/atf/ui/dom/CssPseudoClass.java");
        for (String owner : requiredOwners) assertTrue(owner.startsWith("atf-core/")
                ? CoreGovernanceSurface.read(root).owns(root.resolve(owner)) : Files.isRegularFile(root.resolve(owner)),
                "Reusable vocabulary owner is missing: " + owner);

        if (!AtfGovernanceTestSupport.consumerDeclared(root, "atf-test-practice-shop")) return;
        Path shopLocators = root.resolve(
                "atf-test-practice-shop/src/main/atf/shop/locators/ShopLocators.java");
        String locatorSource = Files.readString(shopLocators);
        Pattern rawHtmlSelector = Pattern.compile(
                "\\\"(?:body|nav|span|h1|input|textarea|select|option|button|a|img|form|table|footer)"
                        + "(?:\\[|\\.|:|\\s|,|\\\")");
        assertFalse(rawHtmlSelector.matcher(locatorSource).find(),
                "Practice Shop locators must compose canonical element names through atf-ui HtmlElement");
        assertFalse(locatorSource.contains(".attributeEquals(\""),
                "Practice Shop locators must compose canonical attribute names through atf-ui HtmlAttribute");
        assertFalse(locatorSource.contains("\":checked\""),
                "Practice Shop locators must compose reusable pseudo-classes through atf-ui CssPseudoClass");
        assertTrue(locatorSource.contains("HtmlElement.INPUT")
                        && locatorSource.contains("HtmlElement.TEXTAREA")
                        && locatorSource.contains("HtmlElement.SELECT")
                        && locatorSource.contains("DATA_TEST")
                        && locatorSource.contains("CssPseudoClass.CHECKED"),
                "Practice Shop locators must reuse the canonical atf-ui DOM vocabulary");

        assertFalse(Files.exists(root.resolve(
                "atf-test-practice-shop/src/main/atf/shop/api/contract/ShopApiAuthType.java")));
        assertFalse(Files.exists(root.resolve(
                "atf-test-practice-shop/src/main/atf/shop/api/contract/ShopApiAuthProfile.java")));
    }

    @Test
    void reportingTestsDoNotRepeatConcreteRouteOrEnvironmentLiterals() throws Exception {
        ReportingHarnessOwnership.read(locateProjectRoot()).inspect(() -> {
            Path root = locateProjectRoot();
            Path reportingTests = root.resolve("atf-reporting-tests/src/test/java");
            var violations = new ArrayList<String>();
            try (Stream<Path> files = Files.walk(reportingTests)) {
                files.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .forEach(path -> {
                            String relative = root.relativize(path).toString().replace('\\', '/');
                            if (LITERAL_OWNER_EXEMPTIONS.contains(relative)) return;
                            String source;
                            try {
                                source = stripTextBlocks(Files.readString(path));
                            } catch (java.io.IOException error) {
                                throw new java.io.UncheckedIOException(error);
                            }
                            if (source.contains("/public/v2/") || source.contains("https://atf.local/")) {
                                violations.add(relative);
                            }
                        });
            }
            assertTrue(violations.isEmpty(), () ->
                    "Reporting tests must use MockRoutePaths/ApiReportTestUrls, not concrete URL literals: " + violations);
        });
    }

    @Test
    void reportingLibraryDoesNotExposeExecutionAsAProductionDependency() throws Exception {
        Path root = locateProjectRoot();
        String pom = ReportingGovernanceSurface.read(root).pom();
        assertFalse(pom.contains("<artifactId>atf-execution</artifactId>"),
                "standalone reporting must not depend on execution; composition belongs to atf-reporting-tests");
    }

    @Test
    void reportingLibraryKeepsValidationAsATestOnlyAdapter() throws Exception {
        Path root = locateProjectRoot();
        String pom = ReportingGovernanceSurface.read(root).pom();
        assertFalse(pom.contains("<artifactId>atf-validation</artifactId>"),
                "standalone reporting must not depend on validation; composition belongs to atf-reporting-tests");
    }

    @Test
    void reportingTestsDoNotImportApplicationRouteCatalog() throws Exception {
        ReportingHarnessOwnership.read(locateProjectRoot()).inspect(() -> {
            Path root = locateProjectRoot();
            Path reportingTests = root.resolve("atf-reporting-tests/src/test/java");
            var violations = new ArrayList<String>();
            try (Stream<Path> files = Files.walk(reportingTests)) {
                files.filter(path -> path.toString().endsWith(".java"))
                        .filter(path -> !path.getFileName().toString().equals("ConstantsOwnershipGovernanceTest.java"))
                        .forEach(path -> {
                    try {
                        String relative = root.relativize(path).toString().replace('\\', '/');
                        if (LITERAL_OWNER_EXEMPTIONS.contains(relative)) return;
                        if (Files.readString(path).contains("import atf.api.execution.routing.RoutePaths;")) {
                            violations.add(relative);
                        }
                    } catch (java.io.IOException error) {
                        throw new java.io.UncheckedIOException(error);
                    }
                });
            }
            assertTrue(violations.isEmpty(), () ->
                    "Reporting tests must use the mock route catalog, not execution RoutePaths: " + violations);
        });
    }

    private static void inspectCanonicalSource(Path root, String relative, List<String> violations) throws Exception {
        if (relative.startsWith("atf-mock/")) {
            var mock = new atf.libraries.PlatformLibrarySurface(root, "mock");
            if (!mock.source()) {
                assertTrue(mock.owns(root.resolve(relative)), "Published Mock class/native catalog is missing " + relative);
                return;
            }
        }
        String content = stripTextBlocks(Files.readString(root.resolve(relative)));
        for (Pattern pattern : FORBIDDEN_CANONICALS) {
            if (pattern.matcher(content).find()) violations.add(relative + " contains unowned canonical literal: " + pattern);
        }
    }

    private static String stripTextBlocks(String source) {
        return source.replaceAll("(?s)\\\"\\\"\\\".*?\\\"\\\"\\\"", "\\\"\\\"\\\"");
    }

}
