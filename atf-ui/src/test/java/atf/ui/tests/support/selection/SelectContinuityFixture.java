package atf.ui.tests.support.selection;

import atf.core.reporting.console.AtfConsoleEvent;
import atf.core.ui.AtfUiActionRecord;
import atf.ui.playwright.*;
import com.microsoft.playwright.*;
import java.lang.reflect.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Local documents plus a counted native port; the fixture never decides the production verdict. */
public final class SelectContinuityFixture implements AutoCloseable {
    public static final String PRIVATE_LABEL = "private \"blue\" <selection>";
    private final PlaywrightUiSession browser;
    private Page page;
    private final PlaywrightActionSequence actions;
    private final String mode, frame, label;
    private final AtomicInteger nativeCalls = new AtomicInteger(), inputs = new AtomicInteger(), changes = new AtomicInteger(), old = new AtomicInteger();
    private final List<AtfConsoleEvent> trace;
    private final int traceStart;
    private final AtfLocator target;
    private final TimeoutError unconfirmed = new TimeoutError("Native selection response was not confirmed");
    private Double nativeTimeout;

    SelectContinuityFixture(SelectContinuityBrowser owner, String mode, String frame, boolean sensitive) throws Exception {
        this.mode = mode; this.frame = frame; label = sensitive ? PRIVATE_LABEL : "Blue";
        browser = owner.session(); trace = owner.trace(); traceStart = trace.size();
        try {
            page = owner.newPage();
            page.exposeFunction("fixtureInput", args -> { inputs.incrementAndGet(); if (Boolean.TRUE.equals(args[0])) old.incrementAndGet(); return null; });
            page.exposeFunction("fixtureChange", args -> { changes.incrementAndGet(); return null; });
            String document;
            try (var input = Objects.requireNonNull(getClass().getResourceAsStream("/atf/ui/playwright/select-continuity.html"))) {
                document = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
            page.route("**/*", route -> {
                URI uri = URI.create(route.request().url());
                String body = document;
                if (!frame.equals("top") && uri.getPath().equals("/main")) {
                    String host = frame.equals("cross") ? "child.select.test" : "select.test";
                    body = "<iframe id='owned-frame' name='owned-frame' src='http://" + host + "/child?" + uri.getRawQuery() + "'></iframe>"
                            + "<script>addEventListener('message',e=>{if(e.data==='remove-owned-select-frame')document.querySelector('iframe').remove()})</script>";
                }
                route.fulfill(new Route.FulfillOptions().setContentType("text/html").setBody(body));
            });
            page.navigate("http://select.test/main?mode=" + mode + "&label=" + URLEncoder.encode(label, StandardCharsets.UTF_8));
            Locator actual = frame.equals("top") ? page.locator("#target") : page.frameLocator("#owned-frame").locator("#target");
            Locator counted = (Locator) Proxy.newProxyInstance(Locator.class.getClassLoader(), new Class<?>[]{Locator.class}, (proxy, method, args) -> {
                if (method.getName().equals("selectOption")) {
                    nativeCalls.incrementAndGet();
                    if (args.length > 1) nativeTimeout = ((Locator.SelectOptionOptions) args[1]).timeout;
                    if (mode.equals("empty")) return List.of();
                    if (mode.equals("native-detach")) owner().evaluate("window.armNativeDetach()");
                }
                try {
                    Object result = method.invoke(actual, args);
                    if (method.getName().equals("selectOption") && mode.equals("unconfirmed")) throw unconfirmed;
                    return result;
                } catch (InvocationTargetException failure) { throw failure.getCause(); }
            });
            var evidence = new AtfLocator(counted, "#target"); target = sensitive ? evidence.withSensitiveData() : evidence;
            actions = browser.newActionSequence();
        } catch (Exception | Error failure) {
            try { close(); } catch (RuntimeException | Error cleanup) { if (cleanup != failure) failure.addSuppressed(cleanup); }
            throw failure;
        }
    }
    public void select() { owner().evaluate("window.arm()"); actions.selectOption(target, label, "configured choice " + label + " is selected once"); }
    private Frame owner() { return frame.equals("top") ? page.mainFrame() : page.frames().stream().filter(f -> f.name().equals("owned-frame")).findFirst().orElseThrow(); }
    public String value() { return (String) owner().evaluate("document.querySelector('#target').value"); }
    public String url() { return owner().url(); }
    public int nativeCalls() { return nativeCalls.get(); }
    public int inputs() { page.title(); return inputs.get(); }
    public int changes() { page.title(); return changes.get(); }
    public int oldActivations() { return old.get(); }
    public Double nativeTimeout() { return nativeTimeout; }
    public TimeoutError unconfirmed() { return unconfirmed; }
    public List<AtfUiActionRecord> records() { return actions.snapshot(); }
    public List<AtfConsoleEvent> trace() { return List.copyOf(trace.subList(traceStart, trace.size())); }
    public Page page() { return page; }
    @Override public void close() { if (page != null) page.close(); }
}
