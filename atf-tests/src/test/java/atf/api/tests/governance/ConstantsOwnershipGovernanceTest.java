package atf.api.tests.governance;
import atf.api.testkit.AtfComponentTest;

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
            "atf-mock/src/main/java/atf/api/mock/AtfMockConfiguration.java",
            "atf-validation/src/main/java/atf/api/validation/contract/ContractValidators.java",
            "atf-validation/src/main/java/atf/api/validation/contract/ResponseContractRuleSet.java",
            "atf-mock/src/test/java/atf/api/mock/AtfMockTransportTest.java",
            "atf-mock/src/test/java/atf/api/mock/MockRouteIdentityTest.java",
            "atf-reporting-tests/src/test/java/atf/api/tests/api/flows/ApiFlowAssertions.java");
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
            "atf-reporting-tests/src/test/java/atf/api/reporting/support/ApiReportTestUrls.java");

    @Test
    void canonicalProtocolAndRouteValuesHaveSemanticOwners() throws Exception {
        Path root = locateProjectRoot();
        var violations = new ArrayList<String>();
        for (String relative : REVIEWED_SOURCES) {
            Path source = root.resolve(relative);
            String content = stripTextBlocks(Files.readString(source));
            for (Pattern pattern : FORBIDDEN_CANONICALS) {
                if (pattern.matcher(content).find()) {
                    violations.add(relative + " contains unowned canonical literal: " + pattern);
                }
            }
        }
        assertTrue(violations.isEmpty(), () -> "Constants ownership violations: " + violations);
    }

    @Test
    void coreKeepsPathMechanicsGenericAndLeavesConcreteCatalogsAtTheirBoundaries() {
        Path root = locateProjectRoot();
        assertTrue(Files.isRegularFile(root.resolve(
                        "atf-core/src/main/java/atf/api/core/routing/ApiPathTemplate.java")),
                "core must expose the generic path-template mechanism");
        assertTrue(!Files.exists(root.resolve(
                        "atf-core/src/main/java/atf/api/core/routing/ApiPaths.java")),
                "core must not own a concrete product route catalog");
        assertTrue(Files.isRegularFile(root.resolve(
                        "atf-reporting-tests/src/test/java/atf/api/execution/routing/RoutePaths.java")));
        assertTrue(Files.isRegularFile(root.resolve(
                        "atf-mock/src/main/java/atf/api/mock/MockRoutePaths.java")));
    }

    @Test
    void reusableProtocolDomAuthAndDiagnosticVocabularyStayWithPlatformOwners() throws Exception {
        Path root = locateProjectRoot();
        List<String> requiredOwners = List.of(
                "atf-core/src/main/java/atf/api/core/http/protocol/ApiUris.java",
                "atf-core/src/main/java/atf/api/core/http/protocol/HttpHeaderProfiles.java",
                "atf-core/src/main/java/atf/api/core/http/auth/AuthProfiles.java",
                "atf-core/src/main/java/atf/api/core/diagnostics/ThrowableDiagnostics.java",
                "atf-ui/src/main/java/atf/api/ui/dom/HtmlElement.java",
                "atf-ui/src/main/java/atf/api/ui/dom/HtmlAttribute.java",
                "atf-ui/src/main/java/atf/api/ui/dom/CssPseudoClass.java");
        assertTrue(requiredOwners.stream().map(root::resolve).allMatch(Files::isRegularFile),
                () -> "Reusable vocabulary owner is missing: " + requiredOwners);

        Path shopLocators = root.resolve(
                "atf-test-practice-shop/src/main/java/atf/shop/locators/ShopLocators.java");
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
                "atf-test-practice-shop/src/main/java/atf/shop/api/contract/ShopApiAuthType.java")));
        assertFalse(Files.exists(root.resolve(
                "atf-test-practice-shop/src/main/java/atf/shop/api/contract/ShopApiAuthProfile.java")));
    }

    @Test
    void reportingTestsDoNotRepeatConcreteRouteOrEnvironmentLiterals() throws Exception {
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
    }

    @Test
    void reportingLibraryDoesNotExposeExecutionAsAProductionDependency() throws Exception {
        Path root = locateProjectRoot();
        String pom = Files.readString(root.resolve("atf-reporting/pom.xml"));
        assertFalse(pom.contains("<artifactId>atf-execution</artifactId>"),
                "standalone reporting must not depend on execution; composition belongs to atf-reporting-tests");
    }

    @Test
    void reportingLibraryKeepsValidationAsATestOnlyAdapter() throws Exception {
        Path root = locateProjectRoot();
        String pom = Files.readString(root.resolve("atf-reporting/pom.xml"));
        assertFalse(pom.contains("<artifactId>atf-validation</artifactId>"),
                "standalone reporting must not depend on validation; composition belongs to atf-reporting-tests");
    }

    @Test
    void reportingTestsDoNotImportApplicationRouteCatalog() throws Exception {
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
    }

    private static String stripTextBlocks(String source) {
        return source.replaceAll("(?s)\\\"\\\"\\\".*?\\\"\\\"\\\"", "\\\"\\\"\\\"");
    }

}
