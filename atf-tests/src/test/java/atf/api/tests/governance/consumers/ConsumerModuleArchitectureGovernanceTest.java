package atf.api.tests.governance.consumers;

import atf.core.files.AtfFileTree;
import atf.core.files.AtfJavaSourceReferences;
import atf.testkit.AtfIntegrationTest;
import atf.api.tests.governance.AtfGovernanceTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps consumer source-set and test-suite architecture under whole-ATF governance. */
@AtfIntegrationTest
public class ConsumerModuleArchitectureGovernanceTest {
    private static final String JAVA_SUFFIX = ".java";

    @Test
    void autoExBusinessPortsRemainIndependentOfBrowserConfigurationRunnerAndPublisher() throws Exception {
        Path business = module("atf-test-auto-ex").resolve("src/main/java/atf/autoex/business");
        assertTrue(Files.isDirectory(business), "the cart pilot must have a real business owner");
        assertTrue(businessViolations(business).isEmpty(), () -> "Business dependencies escaped: " + business);
    }

    @Test
    void businessBoundaryRejectsAnInjectedImportAndFullyQualifiedSignature(@TempDir Path root) throws Exception {
        Path source = root.resolve("BusinessFlow.java");
        for (String code : List.of("import atf.reporting.contract.AtfAssertion;",
                "interface Bad { com.microsoft.playwright.Page page(); }",
                "class Bad { atf.ui.playwright.PlaywrightActionSequence actions; }")) {
            Files.writeString(source, code);
            var violations = businessViolations(root);
            assertTrue(violations.size() == 1 && violations.getFirst().startsWith(source.toString()),
                    () -> "Missing business rule diagnostic: " + violations);
        }
        Files.writeString(source, "// atf.reporting is an example\nclass Good { String key = \"atf.ui.timeout\"; }");
        assertTrue(businessViolations(root).isEmpty());
    }

    private static List<String> businessViolations(Path root) throws IOException {
        var violations = new ArrayList<String>();
        for (Path path : javaFiles(root)) {
            String source = Files.readString(path);
            for (String namespace : List.of("atf.reporting", "atf.ui", "com.microsoft.playwright", "org.junit",
                    "atf.testkit", "atf.autoex.runtime", "atf.autoex.config", "atf.autoex.page",
                    "atf.autoex.support.journey", "atf.autoex.ui", "atf.autoex.flow")) {
                if (AtfJavaSourceReferences.containsNamespace(source, namespace)) {
                    violations.add(path + " violates business dependency rule: " + namespace);
                }
            }
        }
        return List.copyOf(violations);
    }

    @Test
    void shopKeepsJUnitHarnessTestScopedAndRuntimeFactoriesInMain() throws IOException {
        Path module = module("atf-test-practice-shop");
        Path testRoot = module.resolve("src/test/atf/shop/tests");
        Path context = testRoot.resolve("support/lifecycle/ShopE2eContext.java");

        assertTrue(Files.isRegularFile(context), "Shop browser/report context must be test-scoped");
        assertFalse(Files.exists(module.resolve(
                        "src/main/atf/shop/support/ShopE2eContext.java")),
                "browser/report lifecycle must not ship with the Shop runtime");

        for (String helper : List.of("support/reporting/ShopReportSystemMetadata.java")) {
            assertTrue(Files.isRegularFile(testRoot.resolve(helper)),
                    "Shop reporting adapter must remain test-scoped: " + helper);
            assertFalse(Files.exists(module.resolve("src/main/atf/shop/" + helper)),
                    "Shop reporting adapter must not enter the production artifact: " + helper);
        }

        for (String factory : List.of("evidence/ShopBrowserCaptureProvider.java",
                "reporting/ShopScreenshotPolicy.java")) {
            Path runtime = module.resolve("src/main/atf/shop/" + factory);
            assertTrue(Files.isRegularFile(runtime), "Shop runtime factory must have its main owner: " + factory);
            assertFalse(Files.exists(testRoot.resolve("support/" + factory)),
                    "do not restore a duplicate test-scoped runtime factory: " + factory);
            String source = Files.readString(runtime);
            assertFalse(source.contains("org.junit") || source.contains("atf.testkit"),
                    "runtime factories must not pull JUnit or Test Kit into Shop main: " + factory);
        }

        String pom = Files.readString(module.resolve("pom.xml"));
        assertDependencyScope(pom, "atf-test-kit", "test");
        for (String dependency : List.of("atf-core", "atf-console", "atf-reporting", "atf-ui")) {
            assertDependencyScope(pom, dependency, null);
        }
    }

    @Test
    void shopUiSourcesRespectTheLocatorAndSelectorBoundary() throws IOException {
        Path module = module("atf-test-practice-shop");
        Path sourceRoot = module.resolve("src/main/atf/shop");
        Path testRoot = module.resolve("src/test/atf/shop/tests");
        Path locatorRoot = sourceRoot.resolve("locators").toAbsolutePath().normalize();

        for (Path source : javaFiles(sourceRoot, testRoot)) {
            if (source.toAbsolutePath().normalize().startsWith(locatorRoot)) continue;
            String text = Files.readString(source);
            assertFalse(text.contains("page.locator("), "raw locator escaped the facade: " + source);
            assertFalse(text.contains("getByText("), "text selector escaped the facade: " + source);
            assertFalse(text.contains("getByRole("), "role selector escaped the facade: " + source);
            assertFalse(text.matches("(?s).*pressKey\\(page,\\s*\"[^\"]+\",\\s*\"[^\"]+\".*"),
                    "keyboard actions must receive a locator-facade target: " + source);
        }

        String locators = Files.readString(sourceRoot.resolve("locators/ShopLocators.java"));
        assertTrue(locators.contains("HtmlElement.INPUT.attributeEquals(ID, \"email\")"));
        assertTrue(locators.contains("HtmlElement.INPUT.attributeEquals(ID, \"search-query\")"));
        assertTrue(locators.contains("DATA_TEST.equalsSelector(\"login-submit\")"));
        assertTrue(locators.contains("DATA_TEST.equalsSelector(\"register-submit\")"));
        assertFalse(locators.contains("input#"), "ID shorthand must not be the default selector form");
        assertTrue(locators.contains("atf.ui.dom.HtmlElement")
                        && locators.contains("atf.ui.dom.HtmlAttribute"),
                "Shop must reuse the typed DOM vocabulary from atf-ui");
    }

    @Test
    void shopLocalSelectionAndApiAuditKeepExplicitBoundaries() throws IOException {
        Path tests = module("atf-test-practice-shop")
                .resolve("src/test/atf/shop/tests");
        String componentSuite = Files.readString(tests.resolve("suites/ShopComponentSuite.java"));
        for (String selected : List.of("atf.shop.tests.component",
                "atf.shop.tests.api.component",
                "atf.shop.tests.api.unit",
                "atf.shop.tests.api.reporting",
                "atf.shop.tests.api.network",
                "atf.shop.tests.support")) {
            assertTrue(AtfJavaSourceReferences.annotationArguments(componentSuite, "SelectPackages")
                            .orElseThrow().contains("\"" + selected + "\""),
                    "Shop component suite must keep its explicit consumer boundary: " + selected);
        }
        assertFalse(componentSuite.contains("atf.shop.tests.e2e"));
        assertFalse(componentSuite.contains("atf.shop.tests.api.integration"));

        assertFalse(componentSuite.contains("\"atf.shop.support.api\""),
                "Shop API tests belong to the canonical test namespace");
        Path legacy = module("atf-test-practice-shop").resolve(
                "src/test/atf/shop/support/api");
        assertTrue(javaFiles(legacy).isEmpty(), "do not restore tests under the runtime namespace");
        Path network = tests.resolve("api/network");
        for (String contract : List.of("ShopApiNetworkJournalTest", "ShopApiMethodFidelityTest")) {
            String source = Files.readString(network.resolve(contract + ".java"));
            assertTrue(source.startsWith("package atf.shop.tests.api.network;"));
            assertFalse(source.contains(".recordCapturedExchange("),
                    "consumer contracts must exercise public capture subscriptions");
        }
        String journal = Files.readString(module("atf-test-practice-shop").resolve(
                "src/main/atf/shop/support/api/ShopApiNetworkJournal.java"));
        assertTrue(journal.contains("private void recordCapturedExchange("),
                "moving tests must not widen the runtime recorder");
        String context = Files.readString(tests.resolve("support/lifecycle/ShopE2eContext.java"));
        assertTrue(context.contains("ReportModelAdapters.uiActions("),
                "the consumer must call the canonical typed mapper");
        assertFalse(Files.exists(tests.resolve("support/reporting/ShopReportUiActionAdapters.java")),
                "do not restore a pass-through mapper copy");
        String auditDeclaration = Files.readString(tests.resolve(
                "api/support/ShopApiCallComponentTest.java"));
        assertTrue(auditDeclaration.contains("@Tag(\"api-call\")"),
                "request audit remains declared on the component tests, not a redundant suite");
    }

    @Test
    void shopE2eTestsUseObservedAssertionsAndPerMethodHarnesses() throws IOException {
        Path e2e = module("atf-test-practice-shop")
                .resolve("src/test/atf/shop/tests/e2e");
        for (String name : List.of("ShopPurchaseE2eTest", "ShopUiOnlyPurchaseE2eTest",
                "ShopUiOnlySingleFlowPurchaseE2eTest", "ShopAuthenticatedPurchaseE2eTest",
                "ShopNewAccountPurchaseE2eTest")) {
            String source = Files.readString(e2e.resolve(name + ".java"));
            assertTrue(source.contains("@ShopE2eTestSupport.WithE2eHarness"),
                    name + " must use the isolated browser/report extension");
            assertTrue(source.contains("ShopE2eTestSupport.E2eHarness harness"),
                    name + " must receive its per-method harness");
            assertFalse(source.contains("@BeforeEach") || source.contains("@AfterEach"),
                    name + " must not duplicate extension lifecycle callbacks");
            assertFalse(source.contains("@TestInstance") || source.contains("@BeforeAll"),
                    name + " must not share a suite browser");
            assertObservedVerdicts(source, e2e.resolve(name + ".java"), true);
        }

        for (String name : List.of("ShopNewAccountPurchaseE2eTest", "ShopAuthenticatedPurchaseE2eTest")) {
            String source = Files.readString(e2e.resolve(name + ".java"));
            assertTrue(source.contains("ShopAuthenticatedAccountTestSupport::prepare"),
                    name + " must finish account setup before its purchase journey");
            assertTrue(source.contains("purchaseConfiguredProductAuthenticated("),
                    name + " must execute the authenticated purchase path");
            assertTrue(source.contains("journey.completePurchase(purchase);"),
                    name + " must verify the terminal order confirmation");
        }
    }

    @Test
    void autoExKeepsRuntimeInMainAndJUnitAtTheExecutionBoundary() throws IOException {
        Path module = module("atf-test-auto-ex");
        Path main = module.resolve("src/main/java/atf/autoex");
        Path test = module.resolve("src/test/java/atf/autoex/tests");
        Path journey = main.resolve("support/journey/AutoExPurchaseJourney.java");
        Path session = main.resolve("support/journey/AutoExPurchaseSession.java");
        Path cartJourney = main.resolve("support/cart/AutoExCartJourney.java");
        Path cartJourneyPort = main.resolve("support/cart/AutoExCartJourneyPort.java");
        Path catalogSearchPort = main.resolve("support/journey/AutoExCatalogSearchPort.java");
        Path accountPort = main.resolve("ui/AutoExAccountPagePort.java");
        Path checkoutPort = main.resolve("ui/AutoExCheckoutPagePort.java");
        Path homePort = main.resolve("ui/AutoExHomePagePort.java");
        String journeySource = Files.readString(journey);
        String sessionSource = Files.readString(session);
        String cartJourneySource = Files.readString(cartJourney);
        String cartJourneyPortSource = Files.readString(cartJourneyPort);
        String catalogSearchPortSource = Files.readString(catalogSearchPort);
        String accountPortSource = Files.readString(accountPort);
        String checkoutPortSource = Files.readString(checkoutPort);
        String homePortSource = Files.readString(homePort);

        assertTrue(Files.isRegularFile(module.resolve("src/test/resources/atf-auto-ex.yaml")),
                "site-specific E2E configuration belongs to test resources");
        assertFalse(Files.exists(module.resolve("src/main/resources/atf-auto-ex.yaml")),
                "test configuration must not be packaged as a production resource");
        for (String path : List.of("config/AutoExConfigurationLoader.java", "page/AccountPage.java",
                "locator/AutoExSelectors.java")) {
            assertTrue(Files.isRegularFile(main.resolve(path)), "missing AutoEx runtime type: " + path);
        }
        assertTrue(sessionSource.contains("extends AutoExCartJourneyPort")
                        && sessionSource.contains("AutoExCheckoutPagePort"),
                "the purchase session must add checkout to the narrower cart-story port");
        assertTrue(cartJourneySource.contains("AutoExCartJourneyPort")
                        && cartJourneyPortSource.contains("AutoExAccountPagePort")
                        && cartJourneyPortSource.contains("AutoExCatalogSearchPort")
                        && catalogSearchPortSource.contains("AutoExHomePagePort"),
                "the UI cart story must compose only account and catalog-search capabilities");
        assertFalse(cartJourneyPortSource.contains("inspectCatalogApi")
                        || cartJourneyPortSource.contains("apiCheckpoint")
                        || cartJourneyPortSource.contains("AutoExCheckoutPagePort"),
                "UI cart stories must not receive API-inspector or checkout capabilities");
        assertFalse(sessionSource.contains("com.microsoft.playwright.Page"),
                "browser access belongs only to the page ports that need it");
        assertTrue(accountPortSource.contains("AutoExAccountLifecycle")
                        && accountPortSource.contains("onFailureCleanup"),
                "account ownership and cleanup belong to the account-page port");
        assertTrue(checkoutPortSource.contains("com.microsoft.playwright.Page"),
                "checkout may access the browser for its invoice download");
        assertTrue(homePortSource.contains("dismissPrivacyPromptIfPresent"),
                "optional privacy handling belongs to the home-page port");
        for (var pagePort : List.of(
                List.of("page/AccountPage.java", "AutoExAccountPagePort"),
                List.of("page/CheckoutPage.java", "AutoExCheckoutPagePort"),
                List.of("page/HomePage.java", "AutoExHomePagePort"))) {
            assertTrue(Files.readString(main.resolve(pagePort.getFirst())).contains(pagePort.get(1)),
                    pagePort.getFirst() + " must depend on its page-specific port");
        }
        assertTrue(sessionSource.contains("atf.reporting.contract.AtfAssertion"),
                "the AutoEx journey port may expose typed assertions");
        assertFalse(sessionSource.contains("org.junit") || sessionSource.contains("atf.testkit"),
                "JUnit and generic test-kit concerns must remain outside runtime code");
        assertTrue(journeySource.contains("AutoExPurchaseTraceStep."));
        assertFalse(journeySource.contains("checkpoint(AtfGherkinKeyword"),
                "business checkpoint titles must come from the typed phrase catalog");

        for (Path source : javaFiles(main)) {
            String text = Files.readString(source);
            assertFalse(text.contains("org.junit") || text.contains("atf.testkit"),
                    "JUnit/test-kit imports must not leak into AutoEx main sources: " + source);
        }
        assertTrue(Files.isDirectory(test), "AutoEx test source root must exist");
        for (String runtime : List.of("AutoExE2eRun", "AutoExReportSession", "AutoExRunLifecycle")) {
            assertTrue(Files.isRegularFile(main.resolve("runtime/" + runtime + ".java")));
        }
        Path legacy = module.resolve("src/test/java/atf/autoex/support");
        for (String misplaced : List.of("AutoExE2eRun", "AutoExConsoleBridge", "AutoExCustomerApiSetup",
                "AutoExProjectRoot", "AutoExEvidenceProfile", "AutoExE2eExtension")) {
            assertFalse(Files.exists(legacy.resolve(misplaced + ".java")),
                    "support must have an explicit runtime or JUnit owner: " + misplaced);
        }
        Path extensions = test.resolve("extension");
        assertTrue(Files.isRegularFile(extensions.resolve("AutoExE2eExtension.java")));
        assertTrue(Files.readString(extensions.resolve("AutoExRunLifecycleAdapter.java"))
                .contains("FailureEvidenceCleanup.finish("));
        assertDependencyScope(Files.readString(module.resolve("pom.xml")), "atf-test-kit", "test");
    }

    @Test
    void autoExSuitesKeepComponentPurchaseAndLiveApiExecutionSeparate() throws IOException {
        Path tests = module("atf-test-auto-ex").resolve("src/test/java/atf/autoex/tests");
        String component = Files.readString(tests.resolve("suites/AutoExComponentSuite.java"));
        assertTrue(component.contains("@SelectPackages(\"atf.autoex.tests.component\")"));

        String purchase = Files.readString(tests.resolve("suites/AutoExE2eSuite.java"));
        for (String test : List.of("AutoExPurchaseUiE2eTest.class",
                "AutoExPurchaseUiApiE2eTest.class", "AutoExPurchaseUiFinalScreenshotE2eTest.class",
                "AutoExCartUiE2eTest.class", "AutoExAccountUiE2eTest.class",
                "AutoExExistingCustomerUiApiE2eTest.class")) {
            assertTrue(purchase.contains(test), "purchase suite must retain the declared evidence variant: " + test);
        }

        String apiSuite = Files.readString(tests.resolve("suites/AutoExApiLiveSuite.java"));
        String liveContract = Files.readString(tests.resolve("api/AutoExApiLiveContractTest.java"));
        assertTrue(apiSuite.contains("AutoExApiLiveContractTest.class"));
        assertTrue(apiSuite.contains("AutoExApiReadOnlyLiveContractTest.class"));
        String readOnly = Files.readString(tests.resolve("api/AutoExApiReadOnlyLiveContractTest.java"));
        assertTrue(readOnly.contains("atf.autoex.api.readonly.live.enabled"));
        assertFalse(readOnly.contains("AutoExApiLiveContractTest."),
                "live test entry points must not depend on other test classes");
        assertTrue(liveContract.contains("@EnabledIfSystemProperty"));
        assertTrue(liveContract.contains("atf.autoex.api.live.enabled"));
    }

    private static Path module(String name) {
        return AtfGovernanceTestSupport.locateProjectRoot().resolve(name);
    }

    @Test
    void sharedInitializationAndNeutralProofsStayAtTheirOwners() throws IOException {
        Path kit = module("atf-test-kit");
        assertTrue(Files.isRegularFile(kit.resolve(
                "src/main/java/atf/testkit/lifecycle/TestRunInitialization.java")),
                "generic acquisition and rollback belong to Test Kit");
        Path autoEx = module("atf-test-auto-ex");
        assertFalse(Files.exists(autoEx.resolve(
                "src/test/java/atf/autoex/support/lifecycle/AutoExE2eInitialization.java")),
                "do not recreate a consumer copy of generic initialization");
        assertFalse(Files.exists(autoEx.resolve(
                "src/test/java/atf/autoex/tests/component/lifecycle/AutoExE2eInitializationComponentTest.java")),
                "factory-only rollback proofs belong next to the Test Kit primitive");
        assertTrue(Files.readString(autoEx.resolve(
                "src/test/java/atf/autoex/tests/extension/AutoExRunFactory.java"))
                .contains("TestRunInitialization.create("),
                "AutoEx must wire its browser/report callbacks into shared initialization");

        for (Path source : javaFiles(kit.resolve("src/main/java"), kit.resolve("src/test/java"),
                module("atf-reporting-tests").resolve("src/test/java"))) {
            String text = Files.readString(source);
            assertFalse(text.matches("(?s).*\\bimport\\s+(?:static\\s+)?"
                            + "(?:atf\\.shop|atf\\.autoex)\\..*"),
                    "neutral support/proofs must not import consumer classes: " + source);
        }
        for (Path source : javaFiles(kit.resolve("src/main/java"), kit.resolve("src/test/java"))) {
            assertFalse(Files.readString(source).matches(
                            "(?s).*\\bimport\\s+(?:static\\s+)?(?:atf\\.(?:reporting|ui)\\."
                                    + "|com\\.microsoft\\.playwright\\.).*"),
                    "Test Kit must remain independent of Reporting and Playwright: " + source);
        }
        String pom = Files.readString(kit.resolve("pom.xml"));
        for (String forbidden : List.of("atf-reporting", "atf-ui", "atf-reporting-tests",
                "atf-test-practice-shop", "atf-test-auto-ex")) {
            assertFalse(pom.contains("<artifactId>" + forbidden + "</artifactId>"),
                    "Test Kit must not depend on " + forbidden);
        }
    }

    @Test
    void sharedConsumerSupportIsWiredAtItsOwnerWithoutLocalCopies() throws IOException {
        Path kit = module("atf-test-kit");
        for (String primitive : List.of("lifecycle/CloseOnceResource", "lifecycle/ArmedCleanup")) {
            assertTrue(Files.isRegularFile(kit.resolve("src/main/java/atf/testkit/" + primitive + ".java")));
            assertTrue(Files.isRegularFile(kit.resolve("src/test/java/atf/testkit/" + primitive + "Test.java")));
        }
        Path autoEx = module("atf-test-auto-ex").resolve("src/test/java");
        for (String duplicate : List.of("atf/autoex/support/AutoExApiTrafficWindow.java",
                "atf/autoex/support/AutoExScreenAuditObservations.java",
                "atf/autoex/tests/api/AutoExApiAccountCleanup.java",
                "atf/autoex/tests/api/AutoExApiResponseAssertions.java")) {
            assertFalse(Files.exists(autoEx.resolve(duplicate)),
                    "do not restore extracted or misplaced consumer support: " + duplicate);
        }
        String run = Files.readString(module("atf-test-auto-ex").resolve(
                "src/main/java/atf/autoex/runtime/AutoExE2eRun.java"));
        assertTrue(run.contains(".observationsSince(") && run.contains("UiScreenAudit.record("));
        Path shop = module("atf-test-practice-shop").resolve("src/test/atf/shop/tests");
        String support = Files.readString(shop.resolve("support/ShopE2eTestSupport.java"));
        assertTrue(support.contains("new CloseOnceResource<>("));
        assertFalse(support.contains("class E2eResource"), "close-once must not be duplicated in Shop");
        assertTrue(support.contains("ShopPurchaseConfirmation.assertions(result)"));
        assertTrue(Files.isRegularFile(module("atf-ui").resolve(
                "src/main/java/atf/ui/audit/UiScreenAudit.java")));
        assertTrue(Files.isRegularFile(module("atf-reporting-tests").resolve(
                "src/test/java/atf/reporting/ui/failure/UiSetupFailureEvidenceComponentTest.java")));
    }

    private static List<Path> javaFiles(Path... roots) throws IOException {
        var files = new java.util.ArrayList<Path>();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) continue;
            files.addAll(AtfFileTree.regularFiles(root, List.of(Path.of(".")),
                    path -> path.getFileName().toString().endsWith(JAVA_SUFFIX)));
        }
        return files;
    }

    @Test
    void consumerComponentAssertionsUseTheAuditedJUnitBoundary() throws Exception {
        var annotations = atf.api.tests.governance.layers.AtfTestLayerArchitectureTest
                .annotationSources(AtfGovernanceTestSupport.locateProjectRoot());
        for (String name : List.of("atf-test-auto-ex", "atf-test-practice-shop")) {
            var sources = new java.util.HashMap<String, String>();
            for (Path path : javaFiles(module(name).resolve("src/test"))) {
                String source = Files.readString(path);
                var owner = java.util.regex.Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;").matcher(source);
                if (owner.find()) sources.put(owner.group(1) + "." + path.getFileName().toString().replace(".java", ""), source);
            }
            var violations = ConsumerAuditSourceGraph.violations(sources, annotations);
            assertTrue(violations.isEmpty(), () -> name + " audited boundary violations: " + violations);
        }
    }

    @Test
    void auditedAssertionBoundaryDetectsRawCallsWithoutFlaggingComments() {
        assertTrue(hasUnauditedAssertions("import static org.junit.jupiter.api.Assertions.assertEquals;"));
        assertTrue(hasUnauditedAssertions("class Bad { void check() { org.junit.jupiter.api.Assertions.assertTrue(true); } }"));
        assertFalse(hasUnauditedAssertions("// org.junit.jupiter.api.Assertions is only an example\nclass Good {}"));
        assertFalse(hasUnauditedAssertions("import static atf.testkit.assertions.AuditedAssertions.assertEquals;"));
    }

    private static boolean hasUnauditedAssertions(String source) {
        return AtfJavaSourceReferences.containsNamespace(source, "org.junit.jupiter.api.Assertions");
    }

    private static void assertDependencyScope(String pom, String artifact, String expectedScope) {
        String marker = "<artifactId>" + artifact + "</artifactId>";
        int start = pom.indexOf(marker);
        assertTrue(start >= 0, "missing Shop dependency " + artifact);
        int end = pom.indexOf("</dependency>", start);
        assertTrue(end > start, "malformed Shop dependency " + artifact);
        String declaration = pom.substring(start, end);
        if (expectedScope == null) {
            assertFalse(declaration.contains("<scope>test</scope>"),
                    artifact + " must remain available to Shop main sources");
        } else {
            assertTrue(declaration.contains("<scope>" + expectedScope + "</scope>"),
                    artifact + " must use " + expectedScope + " scope");
        }
    }

    private static void assertObservedVerdicts(String source, Path path, boolean e2e) {
        assertFalse(source.matches("(?s).*\\bcheck\\([^;]*,\\s*true\\s*\\).*"),
                "check verdict must derive from an observed value: " + path);
        assertFalse(source.matches("(?s).*new\\s+AtfAssertion\\([^;]*,\\s*true\\s*\\).*"),
                "assertion verdict must derive from an observed value: " + path);
        if (e2e) {
            assertFalse(source.contains("configuration.productName(), configuration.productName()"),
                    "E2E product assertion must not compare a value with itself: " + path);
            assertFalse(source.contains("\"My account\", \"My account\""),
                    "E2E account assertion must not compare a value with itself: " + path);
        }
    }
}
