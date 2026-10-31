package atf.shop.tests.e2e.failures;

import atf.shop.tests.support.business.ShopBusinessFault;
import atf.shop.tests.support.business.ShopBusinessFault.Point;
import atf.testkit.AtfE2eTest;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.testkit.evidence.*;
import atf.testkit.scope.TestRunScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.suite.api.*;
import java.nio.file.*;
import static atf.shop.tests.e2e.failures.ShopBusinessArtifactAudit.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;



import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.*;

/** Explicit live acceptance probe. Nested negative E2E fixtures are deliberately red; this probe verifies their exact cause and artifacts. */
public abstract class ShopBusinessFailureAcceptance {
    private static final Map<String, Observation> OBSERVATIONS = new LinkedHashMap<>();
    private static final List<Map<String,Object>> RESULTS = new ArrayList<>();
    private static final String OPT_IN = "atf.business.failures.live.enabled";
    private ShopBusinessFailureAcceptance() { }

    public static void main(String[] args) throws Exception {
        if(args.length==2 && args[0].equals("audit")) { auditExisting(Path.of(args[1])); return; }
        assertEquals("true", System.getProperty(OPT_IN), "controlled business failures require their own live opt-in");
        assertEquals("true", System.getProperty("atf.practice.shop.e2e.enabled"), "the consumer E2E opt-in remains independent");
        String mode = args.length == 0 ? "all" : args[0];
        boolean boundaries = mode.startsWith("boundaries");
        assertTrue(Set.of("all","individual","suite","boundaries","boundaries-individual","boundaries-suite").contains(mode));
        List<Class<?>> owners = boundaries ? List.of(AfterSetupUi.class, AfterSetupApi.class)
                : List.of(AccountUi.class, AccountApi.class, CartUi.class, CartApi.class, PurchaseUi.class, PurchaseApi.class);
        String selectedCase=System.getProperty("atf.business.probe.case");
        assertTrue(selectedCase==null || owners.stream().anyMatch(owner -> owner.getSimpleName().equals(selectedCase)), "unknown controlled fixture");
        if (!mode.endsWith("suite")) {
            for (Class<?> owner : owners) {
                if (!System.getProperty("atf.business.probe.case", owner.getSimpleName()).equals(owner.getSimpleName())) continue;
                launch("individual-"+owner.getSimpleName(), selectClass(owner), 1, 0);
                boolean api = owner.getAnnotation(AtfTestChannel.class).value() == TestChannel.UI_API;
                String method = owner.getSimpleName().replace("Api", "").replace("Ui", "").toLowerCase(Locale.ROOT);
                Class<?> healthy = boundaries ? (api ? HealthyBoundariesUi.class : HealthyBoundariesApi.class) : (api ? HealthyUi.class : HealthyApi.class);
                launch("following-"+owner.getSimpleName(), selectMethod(healthy,
                        method, atf.shop.tests.support.ShopE2eTestSupport.E2eHarness.class), 0, 1);
            }
        }
        if (!mode.endsWith("individual")) launch(boundaries ? "boundaries-suite" : "suite", selectClass(boundaries ? BoundarySuite.class : FixtureSuite.class), boundaries?2:6, boundaries?2:6);
        atf.console.AtfConsoleReporter.configuredStdout().info("BUSINESS_FAILURE_ACCEPTANCE=PASS; launches="+RESULTS.size());
    }

    private static void launch(String label, DiscoverySelector selector, long failures, long successes) throws Exception {
        Path root = Path.of(System.getProperty("atf.business.probe.root", ".")).toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(root.resolve("atf-ui/pom.xml")), "launch from the reactor root or pass atf.business.probe.root");
        Path directory = Files.createDirectories(root.resolve("artifacts/reports/business-failures/shop/"+label+"-"+UUID.randomUUID()));
        var yaml = new ObjectMapper(new YAMLFactory());
        var document = yaml.readTree(atf.testkit.files.AtfTestResourceReader.readResourceUtf8(ShopBusinessFailureAcceptance.class, "atf-practice-shop.yaml"));
        var consumer = (ObjectNode) document.path("practiceShop");
        ((ObjectNode) consumer.path("reports")).put("ui", directory.resolve("reports").toString());
        ((ObjectNode) consumer.path("reports")).put("api", directory.resolve("reports").toString());
        Path configuration = directory.resolve("consumer.yaml"); yaml.writeValue(configuration.toFile(), document);
        OBSERVATIONS.clear();
        var execution = atf.testkit.lifecycle.ScopedAcceptanceRun.execute("atf.practice.config", configuration.toString(), directory, () -> {
            var listener = new SummaryGeneratingListener();
            LauncherFactory.create().execute(LauncherDiscoveryRequestBuilder.request().selectors(selector)
                    .configurationParameter("junit.jupiter.execution.parallel.enabled", "false").build(), listener);
            return listener.getSummary();
        }, path -> path.toString().endsWith(".html") && !path.toString().contains(".atf-extent-session-"));
        var result = execution.outcome();
        result.getFailures().forEach(f -> atf.console.AtfConsoleReporter.configuredStdout().info("OBSERVED_CAUSE="+f.getException().getClass().getName()+": "+f.getException().getMessage()));
        var row = new LinkedHashMap<String,Object>();
        row.put("launch",label); row.put("expectedFailures",failures); row.put("failures",result.getTestsFailedCount());
        row.put("successes",result.getTestsSucceededCount()); row.put("skipped",result.getTestsSkippedCount());
        List<Path> reports = execution.artifacts();
        row.put("reports", reports.stream().map(Path::toString).toList()); RESULTS.add(row);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(root.resolve("artifacts/reports/business-failures/shop/acceptance-results.json").toFile(),RESULTS);
        row.put("observations",OBSERVATIONS.entrySet().stream().map(entry -> {
            var observed=entry.getValue(); var detail=new LinkedHashMap<String,Object>();
            detail.put("fixture",entry.getKey()); detail.put("point",observed.point.name()); detail.put("negative",observed.negative);
            detail.put("contextCloses",observed.nativeCloses.get()); detail.put("registrations",observed.registrations.get());
            detail.put("cartAdds",observed.additions.get()); detail.put("orders",observed.payments.get()); detail.put("deletions",observed.deletions.get());
            detail.put("faultReached",observed.negative && observed.injections > 0);
            detail.put("cause",observed.primary==null?"":observed.primary.getClass().getName()); return detail;
        }).toList());
        row.put("verified",false);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(directory.resolve("acceptance.json").toFile(),row);
        OBSERVATIONS.values().forEach(Observation::rethrowUnexpectedFailure);
        assertEquals(0, result.getContainersFailedCount(), label+": containers");
        assertEquals(0, result.getTestsSkippedCount(), label+": no skipped acceptance fixture");
        assertEquals(failures,result.getTestsFailedCount(),label+": expected controlled failures only");
        assertEquals(successes,result.getTestsSucceededCount(),label+": healthy successors");
        assertEquals(failures+successes, OBSERVATIONS.size(),label+": every invocation observed");
        assertEquals(1,reports.size(),label+": exactly one HTML per launch");
        for(var observed:OBSERVATIONS.values()) {
            observed.verify();
            if(observed.primary!=null) assertTrue(result.getFailures().stream().anyMatch(f -> f.getException()==observed.primary),
                    label+": JUnit keeps the exact original business cause");
        }
        var artifactCases=OBSERVATIONS.entrySet().stream().map(entry->{
            var observed=entry.getValue(); String owner=entry.getKey().split("-",2)[0];
            return new ArtifactCase(entry.getKey(),owner,"controlled "+observed.point+" observation after owned account setup",
                    observed.negative,observed.expectedCheckpoint(),observed.primary==null?"":observed.primary.getMessage(),
                    observed.fault.captureColor(),observed.point==Point.CART||observed.point==Point.PURCHASE?2:1,observed.point==Point.AFTER_SETUP);
        }).toList();
        var inputs=new LinkedHashMap<String,Object>(); inputs.put("report",reports.getFirst().toString()); inputs.put("cases",artifactCases);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(directory.resolve("artifact-expectations.json").toFile(),inputs);
        row.put("artifactCasesVerified",artifactCases.size());
        row.put("artifactMutantsRejected",verifyArtifact(reports.getFirst(),artifactCases));
        row.put("verified",true);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(directory.resolve("acceptance.json").toFile(),row);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(root.resolve("artifacts/reports/business-failures/shop/acceptance-results.json").toFile(),RESULTS);
        atf.console.AtfConsoleReporter.configuredStdout().info("VERIFIED_LAUNCH="+label+"; failures="+failures+"; successes="+successes+"; report="+reports.getFirst());
    }

    private static void execute(Point point, boolean negative, atf.shop.tests.support.ShopE2eTestSupport.E2eHarness harness, String key) {
        var observation = new Observation(point,negative,harness.context().page(),harness.configuration().api().endpoint()); observation.shop=harness;
        OBSERVATIONS.put(key,observation);
        var fault=new ShopBusinessFault(harness.context().page(),point); observation.fault=fault;
        try {
            harness.run("Business "+point,"controlled "+point+" observation after owned account setup",
                    setup -> {
                        observation.expectedRegistration=harness.accounts().current().isPresent()?0:1;
                        return atf.shop.tests.support.ShopAuthenticatedAccountTestSupport.prepare(setup);
                    },(journey,credentials)->{
                if(point==Point.ACCOUNT || point==Point.AFTER_SETUP) {
                    if(negative && point==Point.ACCOUNT)fault.arm();
                    var actual=new atf.shop.support.validation.ShopPageValidator(journey.locators()).validate(
                            journey.page(),atf.shop.pages.account.ShopAccountPage.class,"My account");
                    var checkpoint=new atf.shop.support.ShopPurchaseCheckpoint(atf.reporting.contract.AtfGherkinKeyword.THEN,
                            "the owned account remains visible after setup",actual.assertions());
                    if(negative && point==Point.ACCOUNT) { assertFalse(actual.passed(),"the real page validator must observe the controlled fault"); journey.checkpoint(checkpoint); throw new IllegalStateException("Page validation failed: "+checkpoint.title()); }
                    assertTrue(actual.passed(),"healthy account contract");
                    var confirmed=atf.shop.support.ShopPurchaseTraceStep.ACCOUNT_SESSION_CONFIRMED;
                    if(point==Point.AFTER_SETUP) {
                        journey.checkpoint(new atf.shop.support.ShopPurchaseCheckpoint(confirmed.keyword(),confirmed.title(),actual.assertions()));
                        if(negative)fault.afterSetup();
                        journey.pass(confirmed,actual.assertions());
                    } else journey.pass(confirmed,actual.assertions());
                } else {
                    if(negative)fault.arm();
                    var result=new atf.shop.flow.ShopPurchaseFlow(journey.locators()).purchaseConfiguredProductAuthenticated(
                            journey.page(),journey.actions(),journey.configuration(),journey::checkpoint,journey.apiNetwork());
                    journey.completePurchase(result);
                }
            });
            if(negative)throw new AssertionError("Controlled business failure was not reached: "+point);
        } catch(RuntimeException|Error primary) {
            observation.primary=primary;
            try { observation.injections=negative?fault.injections():0; }
            catch(RuntimeException|Error diagnostic) { if(diagnostic!=primary)primary.addSuppressed(diagnostic); }
            throw primary;
        }
    }
    public abstract static class Failure {
        protected abstract Point point();
        @Test void businessFailure(atf.shop.tests.support.ShopE2eTestSupport.E2eHarness harness) { execute(point(),true,harness,getClass().getSimpleName()); }
    }
    public abstract static class Healthy {
        @Test @Order(1) void account(atf.shop.tests.support.ShopE2eTestSupport.E2eHarness harness) { execute(Point.ACCOUNT,false,harness,getClass().getSimpleName()+"-account"); }
        @Test @Order(2) void cart(atf.shop.tests.support.ShopE2eTestSupport.E2eHarness harness) { execute(Point.CART,false,harness,getClass().getSimpleName()+"-cart"); }
        @Test @Order(3) void purchase(atf.shop.tests.support.ShopE2eTestSupport.E2eHarness harness) { execute(Point.PURCHASE,false,harness,getClass().getSimpleName()+"-purchase"); }
    }
    public abstract static class HealthyBoundaries {
        @Test void aftersetup(atf.shop.tests.support.ShopE2eTestSupport.E2eHarness harness) { execute(Point.AFTER_SETUP,false,harness,getClass().getSimpleName()+"-aftersetup"); }
    }

    private static final class Observation {
        final Point point; final boolean negative; final Page page;
        final AtomicInteger nativeCloses=new AtomicInteger(), registrations=new AtomicInteger(), additions=new AtomicInteger(), payments=new AtomicInteger(), deletions=new AtomicInteger();
        Throwable primary;
        ShopBusinessFault fault; atf.shop.tests.support.ShopE2eTestSupport.E2eHarness shop; int injections,expectedRegistration;
        Observation(Point point,boolean negative,Page page,java.net.URI configuredOrigin) {
            String origin=configuredOrigin.resolve("/").toString();
            this.point=point;this.negative=negative;this.page=page;
            assertEquals("about:blank",page.url(),"fresh browser per business invocation");
            page.context().onClose(ignored->nativeCloses.incrementAndGet());
            page.onRequest(request->{
                if(!request.url().startsWith(origin))return;
                String path=java.net.URI.create(request.url().split("[?#]",2)[0]).getPath();
                if(request.method().equals("POST") && path.equals("/users/register"))registrations.incrementAndGet();
                if(request.method().equals("POST") && path.startsWith("/carts/") && Objects.toString(request.postData(),"").contains("product_id"))additions.incrementAndGet();
                if(request.method().equals("POST") && path.equals("/invoices"))payments.incrementAndGet();
                if(request.method().equals("DELETE") && path.startsWith("/users"))deletions.incrementAndGet();
            });
        }
        void rethrowUnexpectedFailure() {
            boolean intended=negative && injections==1 && primary!=null && primary.getClass()==IllegalStateException.class
                    && Objects.toString(primary.getMessage(),"").contains(point==Point.AFTER_SETUP?ShopBusinessFault.AFTER_SETUP_MESSAGE:"Page validation failed: "+expectedCheckpoint());
            if(primary!=null && !intended) {
                if(primary instanceof RuntimeException runtime)throw runtime;
                if(primary instanceof Error error)throw error;
            }
        }
        void verify() {
            rethrowUnexpectedFailure();
            assertEquals(1,nativeCloses.get(),point+": real context closes exactly once");
            assertTrue(page.isClosed(),point+": no surviving browser page");
            assertEquals(negative,primary!=null,point+": original failure only in negative fixture");
            assertEquals(negative?1:0,injections,point+": intended DOM failure reached exactly once");
            assertEquals(expectedRegistration,registrations.get(),point+": account acquisition is not replayed");
            assertEquals(0,deletions.get(),point+": Shop forgets scoped references; it never deletes a borrowed account");
            assertTrue(shop.accounts().isClosed(),point+": account references released at launch owner close");
            if(negative) {
                assertEquals(IllegalStateException.class,primary.getClass(),point+": real page validation cause");
                assertTrue(Objects.toString(primary.getMessage(),"").contains(point==Point.AFTER_SETUP?ShopBusinessFault.AFTER_SETUP_MESSAGE:"Page validation failed:"),point+": business failure remains primary");
                String expectedCheckpoint=point==Point.AFTER_SETUP?ShopBusinessFault.AFTER_SETUP_MESSAGE:expectedCheckpoint();
                assertTrue(primary.getMessage().contains(expectedCheckpoint),point+": the exact intended business checkpoint fails");
                assertEquals(0,primary.getSuppressed().length,point+": no hidden cleanup/report failure");
            }
            boolean accountOnly=point==Point.ACCOUNT || point==Point.AFTER_SETUP;
            assertEquals(accountOnly?0:1,additions.get(),point+": cart add is never replayed");
            int expectedOrders=accountOnly||negative&&point==Point.CART?0:1;
            assertEquals(expectedOrders,payments.get(),point+": confirmation cannot run past a failed cart gate");
        }
        String expectedCheckpoint() {
            return switch(point) {
                case ACCOUNT -> "the owned account remains visible after setup";
                case CART -> "cart displays the configured product with the requested quantity";
                case PURCHASE -> "order confirmation is displayed for the completed purchase";
                case AFTER_SETUP -> atf.shop.support.ShopPurchaseTraceStep.ACCOUNT_SESSION_CONFIRMED.title();
            };
        }
    }
    @Order(1) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI)
    @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @AtfE2eTest @atf.shop.tests.support.ShopE2eTestSupport.WithE2eHarness
    @atf.shop.tests.support.ShopTestSupport.AuthenticatedE2e(accountSetup=atf.shop.tests.support.ShopTestSupport.AccountSetupMode.REGISTER_PER_RUN,
            registrationProfile=atf.shop.tests.support.ShopTestSupport.RegistrationProfileMode.SYNTHETIC)
    @atf.shop.tests.support.ShopTestSupport.AllowsRegistrationPerRun
    public static class AccountUi extends Failure { @Override protected Point point() { return Point.ACCOUNT; } }
    @Order(2) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI_API)
    @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @AtfE2eTest @atf.shop.tests.support.ShopE2eTestSupport.WithE2eHarness
    @atf.shop.tests.support.ShopTestSupport.AuthenticatedE2e(accountSetup=atf.shop.tests.support.ShopTestSupport.AccountSetupMode.REGISTER_PER_RUN,
            registrationProfile=atf.shop.tests.support.ShopTestSupport.RegistrationProfileMode.SYNTHETIC)
    @atf.shop.tests.support.ShopTestSupport.AllowsRegistrationPerRun
    public static class AccountApi extends Failure { @Override protected Point point() { return Point.ACCOUNT; } }
    @Order(3) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI)
    @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @AtfE2eTest @atf.shop.tests.support.ShopE2eTestSupport.WithE2eHarness
    @atf.shop.tests.support.ShopTestSupport.AuthenticatedE2e(accountSetup=atf.shop.tests.support.ShopTestSupport.AccountSetupMode.REGISTER_PER_RUN,
            registrationProfile=atf.shop.tests.support.ShopTestSupport.RegistrationProfileMode.SYNTHETIC)
    @atf.shop.tests.support.ShopTestSupport.AllowsRegistrationPerRun
    public static class CartUi extends Failure { @Override protected Point point() { return Point.CART; } }
    @Order(4) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI_API)
    @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @AtfE2eTest @atf.shop.tests.support.ShopE2eTestSupport.WithE2eHarness
    @atf.shop.tests.support.ShopTestSupport.AuthenticatedE2e(accountSetup=atf.shop.tests.support.ShopTestSupport.AccountSetupMode.REGISTER_PER_RUN,
            registrationProfile=atf.shop.tests.support.ShopTestSupport.RegistrationProfileMode.SYNTHETIC)
    @atf.shop.tests.support.ShopTestSupport.AllowsRegistrationPerRun
    public static class CartApi extends Failure { @Override protected Point point() { return Point.CART; } }
    @Order(5) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI)
    @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @AtfE2eTest @atf.shop.tests.support.ShopE2eTestSupport.WithE2eHarness
    @atf.shop.tests.support.ShopTestSupport.AuthenticatedE2e(accountSetup=atf.shop.tests.support.ShopTestSupport.AccountSetupMode.REGISTER_PER_RUN,
            registrationProfile=atf.shop.tests.support.ShopTestSupport.RegistrationProfileMode.SYNTHETIC)
    @atf.shop.tests.support.ShopTestSupport.AllowsRegistrationPerRun
    public static class PurchaseUi extends Failure { @Override protected Point point() { return Point.PURCHASE; } }
    @Order(6) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI_API)
    @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @AtfE2eTest @atf.shop.tests.support.ShopE2eTestSupport.WithE2eHarness
    @atf.shop.tests.support.ShopTestSupport.AuthenticatedE2e(accountSetup=atf.shop.tests.support.ShopTestSupport.AccountSetupMode.REGISTER_PER_RUN,
            registrationProfile=atf.shop.tests.support.ShopTestSupport.RegistrationProfileMode.SYNTHETIC)
    @atf.shop.tests.support.ShopTestSupport.AllowsRegistrationPerRun
    public static class PurchaseApi extends Failure { @Override protected Point point() { return Point.PURCHASE; } }
    @Order(7) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI)
    @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @AtfE2eTest @atf.shop.tests.support.ShopE2eTestSupport.WithE2eHarness
    @atf.shop.tests.support.ShopTestSupport.AuthenticatedE2e(accountSetup=atf.shop.tests.support.ShopTestSupport.AccountSetupMode.REGISTER_PER_RUN,
            registrationProfile=atf.shop.tests.support.ShopTestSupport.RegistrationProfileMode.SYNTHETIC)
    @atf.shop.tests.support.ShopTestSupport.AllowsRegistrationPerRun
    public static class HealthyUi extends Healthy { }
    @Order(8) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI_API)
    @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @AtfE2eTest @atf.shop.tests.support.ShopE2eTestSupport.WithE2eHarness
    @atf.shop.tests.support.ShopTestSupport.AuthenticatedE2e(accountSetup=atf.shop.tests.support.ShopTestSupport.AccountSetupMode.REGISTER_PER_RUN,
            registrationProfile=atf.shop.tests.support.ShopTestSupport.RegistrationProfileMode.SYNTHETIC)
    @atf.shop.tests.support.ShopTestSupport.AllowsRegistrationPerRun
    public static class HealthyApi extends Healthy { }
    @Order(1) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI) @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @AtfE2eTest @atf.shop.tests.support.ShopE2eTestSupport.WithE2eHarness
    @atf.shop.tests.support.ShopTestSupport.AuthenticatedE2e(accountSetup=atf.shop.tests.support.ShopTestSupport.AccountSetupMode.REGISTER_PER_RUN,
            registrationProfile=atf.shop.tests.support.ShopTestSupport.RegistrationProfileMode.SYNTHETIC)
    @atf.shop.tests.support.ShopTestSupport.AllowsRegistrationPerRun
    public static class AfterSetupUi extends Failure { @Override protected Point point() { return Point.AFTER_SETUP; } }
    @Order(2) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI_API) @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @AtfE2eTest @atf.shop.tests.support.ShopE2eTestSupport.WithE2eHarness
    @atf.shop.tests.support.ShopTestSupport.AuthenticatedE2e(accountSetup=atf.shop.tests.support.ShopTestSupport.AccountSetupMode.REGISTER_PER_RUN,
            registrationProfile=atf.shop.tests.support.ShopTestSupport.RegistrationProfileMode.SYNTHETIC)
    @atf.shop.tests.support.ShopTestSupport.AllowsRegistrationPerRun
    public static class AfterSetupApi extends Failure { @Override protected Point point() { return Point.AFTER_SETUP; } }
    @Order(3) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI) @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @AtfE2eTest @atf.shop.tests.support.ShopE2eTestSupport.WithE2eHarness
    @atf.shop.tests.support.ShopTestSupport.AuthenticatedE2e(accountSetup=atf.shop.tests.support.ShopTestSupport.AccountSetupMode.REGISTER_PER_RUN,
            registrationProfile=atf.shop.tests.support.ShopTestSupport.RegistrationProfileMode.SYNTHETIC)
    @atf.shop.tests.support.ShopTestSupport.AllowsRegistrationPerRun
    public static class HealthyBoundariesUi extends HealthyBoundaries { }
    @Order(4) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI_API) @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @AtfE2eTest @atf.shop.tests.support.ShopE2eTestSupport.WithE2eHarness
    @atf.shop.tests.support.ShopTestSupport.AuthenticatedE2e(accountSetup=atf.shop.tests.support.ShopTestSupport.AccountSetupMode.REGISTER_PER_RUN,
            registrationProfile=atf.shop.tests.support.ShopTestSupport.RegistrationProfileMode.SYNTHETIC)
    @atf.shop.tests.support.ShopTestSupport.AllowsRegistrationPerRun
    public static class HealthyBoundariesApi extends HealthyBoundaries { }
    @Suite @AtfE2eTest
    @SelectClasses({AfterSetupUi.class,AfterSetupApi.class,HealthyBoundariesUi.class,HealthyBoundariesApi.class})
    @ConfigurationParameter(key=TestRunScope.CONFIGURATION_PARAMETER,value="suite")
    @ConfigurationParameter(key="junit.jupiter.testclass.order.default",value="org.junit.jupiter.api.ClassOrderer$OrderAnnotation")
    @ConfigurationParameter(key="junit.jupiter.execution.parallel.enabled",value="false")
    public static class BoundarySuite { }
    @Suite @AtfE2eTest
    @SelectClasses({AccountUi.class,AccountApi.class,CartUi.class,CartApi.class,PurchaseUi.class,PurchaseApi.class,HealthyUi.class,HealthyApi.class})
    @ConfigurationParameter(key=TestRunScope.CONFIGURATION_PARAMETER,value="suite")
    @ConfigurationParameter(key="junit.jupiter.testclass.order.default",value="org.junit.jupiter.api.ClassOrderer$OrderAnnotation")
    @ConfigurationParameter(key="junit.jupiter.execution.parallel.enabled",value="false")
    public static class FixtureSuite { }
}
