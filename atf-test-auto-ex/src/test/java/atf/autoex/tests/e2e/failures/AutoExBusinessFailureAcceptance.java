package atf.autoex.tests.e2e.failures;

import atf.autoex.tests.support.business.AutoExBusinessFault;
import atf.autoex.tests.support.business.AutoExBusinessFault.Point;
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
import static atf.autoex.tests.e2e.failures.AutoExBusinessArtifactAudit.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;



import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.*;

/** Explicit live acceptance probe. Nested negative E2E fixtures are deliberately red; this probe verifies their exact cause and artifacts. */
public abstract class AutoExBusinessFailureAcceptance {
    private static final Map<String, Observation> OBSERVATIONS = new LinkedHashMap<>();
    private static final List<Map<String,Object>> RESULTS = new ArrayList<>();
    private static final String OPT_IN = "atf.business.failures.live.enabled";
    private AutoExBusinessFailureAcceptance() { }

    public static void main(String[] args) throws Exception {
        if(args.length==2 && args[0].equals("audit")) { auditExisting(Path.of(args[1])); return; }
        assertEquals("true", System.getProperty(OPT_IN), "controlled business failures require their own live opt-in");
        assertEquals("true", System.getProperty("atf.autoex.e2e.enabled"), "the consumer E2E opt-in remains independent");
        String mode = args.length == 0 ? "all" : args[0];
        boolean boundaries = mode.startsWith("boundaries");
        assertTrue(Set.of("all","individual","suite","boundaries","boundaries-individual","boundaries-suite").contains(mode));
        List<Class<?>> owners = boundaries ? List.of(RecalculationUi.class, RecalculationApi.class, RemovalUi.class, RemovalApi.class, AfterSetupUi.class, AfterSetupApi.class)
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
                        method, atf.autoex.runtime.AutoExE2eRun.class), 0, 1);
            }
        }
        if (!mode.endsWith("individual")) launch(boundaries ? "boundaries-suite" : "suite", selectClass(boundaries ? BoundarySuite.class : FixtureSuite.class), 6, 6);
        atf.console.AtfConsoleReporter.configuredStdout().info("BUSINESS_FAILURE_ACCEPTANCE=PASS; launches="+RESULTS.size());
    }

    private static void launch(String label, DiscoverySelector selector, long failures, long successes) throws Exception {
        Path root = Path.of(System.getProperty("atf.business.probe.root", ".")).toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(root.resolve("atf-ui/pom.xml")), "launch from the reactor root or pass atf.business.probe.root");
        Path directory = Files.createDirectories(root.resolve("artifacts/reports/business-failures/autoex/"+label+"-"+UUID.randomUUID()));
        var yaml = new ObjectMapper(new YAMLFactory());
        var document = yaml.readTree(atf.testkit.files.AtfTestResourceReader.readResourceUtf8(AutoExBusinessFailureAcceptance.class, "atf-auto-ex.yaml"));
        var consumer = (ObjectNode) document.path("automationExercise");
        ((ObjectNode) consumer.path("reports")).put("uiBaseDirectory", root.relativize(directory.resolve("reports")).toString());
        Path configuration = directory.resolve("consumer.yaml"); yaml.writeValue(configuration.toFile(), document);
        OBSERVATIONS.clear();
        var execution = atf.testkit.lifecycle.ScopedAcceptanceRun.execute("atf.autoex.config", configuration.toString(), directory, () -> {
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
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(root.resolve("artifacts/reports/business-failures/autoex/acceptance-results.json").toFile(),RESULTS);
        row.put("observations",OBSERVATIONS.entrySet().stream().map(entry -> {
            var observed=entry.getValue(); var detail=new LinkedHashMap<String,Object>();
            detail.put("fixture",entry.getKey()); detail.put("point",observed.point.name()); detail.put("negative",observed.negative);
            detail.put("contextCloses",observed.nativeCloses.get()); detail.put("registrations",observed.registrations.get());
            detail.put("cartAdds",observed.additions.get()); detail.put("orders",observed.payments.get());
            detail.put("cartRemovals",observed.removals.get()); detail.put("failureCleanupCalls",observed.fault.cleanupCalls());
            detail.put("deletionRequestEvents",observed.deletions.get()); detail.put("deletionNavigations",observed.deletionNavigations.get());
            detail.put("faultReached",observed.negative && observed.fault.injected());
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
            atf.console.AtfConsoleReporter.configuredStdout().info("MUTATION_WIRE="+observed.wire);
            observed.verify();
            if(observed.primary!=null) assertTrue(result.getFailures().stream().anyMatch(f -> f.getException()==observed.primary),
                    label+": JUnit keeps the exact original business cause");
        }
        var artifactCases=OBSERVATIONS.entrySet().stream().map(entry->{
            var observed=entry.getValue(); String[] identity=entry.getKey().split("-",2);
            return new ArtifactCase(entry.getKey(),identity[0],(observed.negative?"businessFailure":identity[1])+"(AutoExE2eRun)",
                    observed.negative,observed.expectedCheckpoint(),observed.primary==null?"":observed.primary.getMessage(),
                    observed.fault.captureColor(),1,observed.point==Point.AFTER_SETUP);
        }).toList();
        var inputs=new LinkedHashMap<String,Object>(); inputs.put("report",reports.getFirst().toString()); inputs.put("cases",artifactCases);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(directory.resolve("artifact-expectations.json").toFile(),inputs);
        row.put("artifactCasesVerified",artifactCases.size());
        row.put("artifactMutantsRejected",verifyArtifact(reports.getFirst(),artifactCases));
        row.put("verified",true);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(directory.resolve("acceptance.json").toFile(),row);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(root.resolve("artifacts/reports/business-failures/autoex/acceptance-results.json").toFile(),RESULTS);
        atf.console.AtfConsoleReporter.configuredStdout().info("VERIFIED_LAUNCH="+label+"; failures="+failures+"; successes="+successes+"; report="+reports.getFirst());
    }

    private static void execute(Point point, boolean negative, atf.autoex.runtime.AutoExE2eRun run, String key) {
        var observation = new Observation(point,negative,run.page(),run.configuration().baseUrl()); observation.auto=run;
        OBSERVATIONS.put(key,observation);
        var fault = new AutoExBusinessFault(run.page(),point); observation.fault=fault;
        var session = negative ? fault.session(run) : (atf.autoex.support.journey.AutoExPurchaseSession) run;
        try {
            switch(point) {
                case ACCOUNT, AFTER_SETUP -> atf.autoex.support.journey.AutoExAccountJourney.registersAndDeletesOwnAccount(session);
                case CART -> atf.autoex.support.cart.AutoExCartJourney.preservesGuestCartAfterLogin(session);
                case RECALCULATION, REMOVAL -> atf.autoex.support.cart.AutoExCartJourney.recalculatesAndRemovesProduct(session);
                case PURCHASE -> atf.autoex.support.journey.AutoExPurchaseJourney.execute(session);
            }
            if(negative)throw new AssertionError("Controlled business failure was not reached: "+point);
        } catch(RuntimeException|Error primary) { observation.primary=primary; throw primary; }
    }
    public abstract static class Failure {
        protected abstract Point point();
        @Test void businessFailure(atf.autoex.runtime.AutoExE2eRun run) { execute(point(),true,run,getClass().getSimpleName()); }
    }
    public abstract static class Healthy {
        @Test @Order(1) void account(atf.autoex.runtime.AutoExE2eRun run) { execute(Point.ACCOUNT,false,run,getClass().getSimpleName()+"-account"); }
        @Test @Order(2) void cart(atf.autoex.runtime.AutoExE2eRun run) { execute(Point.CART,false,run,getClass().getSimpleName()+"-cart"); }
        @Test @Order(3) void purchase(atf.autoex.runtime.AutoExE2eRun run) { execute(Point.PURCHASE,false,run,getClass().getSimpleName()+"-purchase"); }
    }
    public abstract static class HealthyBoundaries {
        @Test @Order(1) void recalculation(atf.autoex.runtime.AutoExE2eRun run) { execute(Point.RECALCULATION,false,run,getClass().getSimpleName()+"-recalculation"); }
        @Test @Order(2) void removal(atf.autoex.runtime.AutoExE2eRun run) { execute(Point.REMOVAL,false,run,getClass().getSimpleName()+"-removal"); }
        @Test @Order(3) void aftersetup(atf.autoex.runtime.AutoExE2eRun run) { execute(Point.AFTER_SETUP,false,run,getClass().getSimpleName()+"-aftersetup"); }
    }

    private static final class Observation {
        final Point point; final boolean negative; final Page page;
        final AtomicInteger nativeCloses=new AtomicInteger(), registrations=new AtomicInteger(), additions=new AtomicInteger(), payments=new AtomicInteger(), deletions=new AtomicInteger();
        final AtomicInteger deletionNavigations=new AtomicInteger(), unexpectedDeletionRequests=new AtomicInteger();
        final AtomicInteger removals=new AtomicInteger();
        final List<String> wire=new ArrayList<>();
        Throwable primary;
        AutoExBusinessFault fault; atf.autoex.runtime.AutoExE2eRun auto;
        Observation(Point point,boolean negative,Page page,java.net.URI configuredOrigin) {
            String origin=configuredOrigin.resolve("/").toString();
            this.point=point;this.negative=negative;this.page=page;
            assertEquals("about:blank",page.url(),"fresh browser per business invocation");
            page.context().onClose(ignored->nativeCloses.incrementAndGet());
            page.onRequest(request->{
                if(!request.url().startsWith(origin))return;
                String path=java.net.URI.create(request.url().split("[?#]",2)[0]).getPath();
                if(request.method().equals("POST") || path.startsWith("/add_to_cart/") || path.startsWith("/delete_cart/") || path.equals("/delete_account"))wire.add(request.method()+" "+path);
                String form=Objects.toString(request.postData(), "");
                if(request.method().equals("POST") && path.equals("/signup") && form.contains("password=") && form.contains("address1="))registrations.incrementAndGet();
                if(path.startsWith("/add_to_cart/"))additions.incrementAndGet();
                if(path.startsWith("/delete_cart/"))removals.incrementAndGet();
                if(request.method().equals("POST") && path.equals("/payment"))payments.incrementAndGet();
                if(path.equals("/delete_account")) {
                    deletions.incrementAndGet();
                    if(request.isNavigationRequest())deletionNavigations.incrementAndGet();
                    boolean expectedKind=request.isNavigationRequest()?request.resourceType().equals("document"):request.resourceType().equals("other");
                    if(!request.method().equals("GET") || !expectedKind)unexpectedDeletionRequests.incrementAndGet();
                }
            });
        }
        void rethrowUnexpectedFailure() {
            boolean intended=negative && fault.injected() && primary!=null
                    && primary.getClass().getSimpleName().equals(point==Point.AFTER_SETUP?"IllegalStateException":"UiCheckpointFailure")
                    && Objects.toString(primary.getMessage(),"").contains(point==Point.AFTER_SETUP?AutoExBusinessFault.AFTER_SETUP_MESSAGE:expectedCheckpoint());
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
            assertEquals(negative,fault.injected(),point+": intended DOM failure was reached");
            boolean guest = point==Point.RECALCULATION || point==Point.REMOVAL;
            assertEquals(guest?0:1,registrations.get(),point+": account creation is confined to the selected story");
            assertEquals(guest?0:1,deletionNavigations.get(),point+": owned deletion navigation is never invented or replayed");
            assertEquals(0,unexpectedDeletionRequests.get(),point+": supplementary events retain their explicit OTHER classification");
            assertEquals(!guest,auto.accountLifecycle().deletionWasConfirmed(),point+": only an owned account can confirm cleanup");
            assertEquals(!guest,auto.accountLifecycle().creationWasAttempted(),point+": guest cart never acquires account ownership");
            if(negative) {
                assertEquals(guest?0:1,fault.cleanupCalls(),point+": only registered owned cleanup is invoked");
                assertEquals(point==Point.AFTER_SETUP?"IllegalStateException":"UiCheckpointFailure",primary.getClass().getSimpleName(),point+": exact intended cause");
                String expectedCheckpoint=point==Point.AFTER_SETUP?AutoExBusinessFault.AFTER_SETUP_MESSAGE:expectedCheckpoint();
                assertTrue(primary.getMessage().contains(expectedCheckpoint),point+": the exact intended business checkpoint fails");
                assertEquals(0,primary.getSuppressed().length,point+": no hidden cleanup/report failure");
            }
            assertEquals(guest?2:point==Point.ACCOUNT||point==Point.AFTER_SETUP?0:1,additions.get(),point+": cart add is never replayed");
            assertEquals(guest&&!(negative&&point==Point.RECALCULATION)?1:0,removals.get(),point+": failed recalculation prevents removal; an executed removal is never replayed");
            assertEquals(point==Point.PURCHASE?1:0,payments.get(),point+": payment runs only at the purchase boundary");
        }
        String expectedCheckpoint() {
            return switch(point) {
                case ACCOUNT -> "the disposable customer signs in with the same identity";
                case CART -> "signing in preserves the guest cart and its calculated total";
                case PURCHASE -> "the demo checkout confirms the order";
                case RECALCULATION -> "adding the same product updates its quantity and calculated total";
                case REMOVAL -> "removing the selected product leaves the cart empty";
                case AFTER_SETUP -> "the disposable customer account is created";
            };
        }
    }
    @Order(1) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI)
    @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @atf.autoex.tests.extension.AutoExE2eTest @atf.autoex.tests.extension.AutoExNetworkDocuments({"/"})
    public static class AccountUi extends Failure { @Override protected Point point() { return Point.ACCOUNT; } }
    @Order(2) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI_API)
    @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @atf.autoex.tests.extension.AutoExE2eTest @atf.autoex.tests.extension.AutoExNetworkDocuments({"/"})
    public static class AccountApi extends Failure { @Override protected Point point() { return Point.ACCOUNT; } }
    @Order(3) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI)
    @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @atf.autoex.tests.extension.AutoExE2eTest @atf.autoex.tests.extension.AutoExNetworkDocuments({"/"})
    public static class CartUi extends Failure { @Override protected Point point() { return Point.CART; } }
    @Order(4) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI_API)
    @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @atf.autoex.tests.extension.AutoExE2eTest @atf.autoex.tests.extension.AutoExNetworkDocuments({"/"})
    public static class CartApi extends Failure { @Override protected Point point() { return Point.CART; } }
    @Order(5) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI)
    @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @atf.autoex.tests.extension.AutoExE2eTest @atf.autoex.tests.extension.AutoExNetworkDocuments({"/"})
    public static class PurchaseUi extends Failure { @Override protected Point point() { return Point.PURCHASE; } }
    @Order(6) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI_API)
    @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @atf.autoex.tests.extension.AutoExE2eTest @atf.autoex.tests.extension.AutoExNetworkDocuments({"/"})
    public static class PurchaseApi extends Failure { @Override protected Point point() { return Point.PURCHASE; } }
    @Order(7) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI)
    @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @atf.autoex.tests.extension.AutoExE2eTest @atf.autoex.tests.extension.AutoExNetworkDocuments({"/"})
    public static class HealthyUi extends Healthy { }
    @Order(8) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI_API)
    @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @atf.autoex.tests.extension.AutoExE2eTest @atf.autoex.tests.extension.AutoExNetworkDocuments({"/"})
    public static class HealthyApi extends Healthy { }
    @Order(1) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI) @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @atf.autoex.tests.extension.AutoExE2eTest @atf.autoex.tests.extension.AutoExNetworkDocuments({"/"})
    public static class RecalculationUi extends Failure { @Override protected Point point() { return Point.RECALCULATION; } }
    @Order(2) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI_API) @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @atf.autoex.tests.extension.AutoExE2eTest @atf.autoex.tests.extension.AutoExNetworkDocuments({"/"})
    public static class RecalculationApi extends Failure { @Override protected Point point() { return Point.RECALCULATION; } }
    @Order(3) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI) @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @atf.autoex.tests.extension.AutoExE2eTest @atf.autoex.tests.extension.AutoExNetworkDocuments({"/"})
    public static class RemovalUi extends Failure { @Override protected Point point() { return Point.REMOVAL; } }
    @Order(4) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI_API) @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @atf.autoex.tests.extension.AutoExE2eTest @atf.autoex.tests.extension.AutoExNetworkDocuments({"/"})
    public static class RemovalApi extends Failure { @Override protected Point point() { return Point.REMOVAL; } }
    @Order(5) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI) @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @atf.autoex.tests.extension.AutoExE2eTest @atf.autoex.tests.extension.AutoExNetworkDocuments({"/"})
    public static class AfterSetupUi extends Failure { @Override protected Point point() { return Point.AFTER_SETUP; } }
    @Order(6) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI_API) @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @atf.autoex.tests.extension.AutoExE2eTest @atf.autoex.tests.extension.AutoExNetworkDocuments({"/"})
    public static class AfterSetupApi extends Failure { @Override protected Point point() { return Point.AFTER_SETUP; } }
    @Order(7) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI) @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @atf.autoex.tests.extension.AutoExE2eTest @atf.autoex.tests.extension.AutoExNetworkDocuments({"/"})
    public static class HealthyBoundariesUi extends HealthyBoundaries { }
    @Order(8) @EnabledIfSystemProperty(named=OPT_IN,matches="true")
    @AtfTestChannel(TestChannel.UI_API) @AtfUiEvidence(screenshots=AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @atf.autoex.tests.extension.AutoExE2eTest @atf.autoex.tests.extension.AutoExNetworkDocuments({"/"})
    public static class HealthyBoundariesApi extends HealthyBoundaries { }
    @Suite @AtfE2eTest
    @SelectClasses({RecalculationUi.class,RecalculationApi.class,RemovalUi.class,RemovalApi.class,AfterSetupUi.class,AfterSetupApi.class,HealthyBoundariesUi.class,HealthyBoundariesApi.class})
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
