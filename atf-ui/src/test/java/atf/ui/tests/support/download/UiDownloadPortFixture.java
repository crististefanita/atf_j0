package atf.ui.tests.support.download;

import atf.ui.playwright.*;
import com.microsoft.playwright.*;
import java.lang.reflect.Proxy;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Independent browser ports inject acquisition, action and cleanup failures without renderer side effects. */
public final class UiDownloadPortFixture {
    private Consumer<Download> listener;
    private int clicks, removals, readiness;
    private final PlaywrightActionSequence actions = new PlaywrightActionSequence();
    private final Throwable primary;
    private final Throwable cleanup;
    private final String mode;
    public UiDownloadPortFixture(String mode) {
        this.mode = mode;
        primary = new IllegalStateException("controlled " + mode);
        cleanup = mode.equals("same") ? primary : new AssertionError("cleanup");
    }
    public Download run() {
        Download download = proxy(Download.class, (name, args) -> null);
        Page page = proxy(Page.class, (name, args) -> {
            if (name.equals("onDownload")) {
                listener = callback(args);
                if (mode.equals("attach")) throw primary;
            } else if (name.equals("offDownload")) {
                removals++; listener = null;
                if (mode.equals("same") || mode.equals("distinct") || mode.equals("cleanup")) throw cleanup;
            } else if (name.equals("waitForCondition") && !((BooleanSupplier) args[0]).getAsBoolean()) throw primary;
            return null;
        });
        Locator locator = proxy(Locator.class, (name, args) -> {
            if (name.equals("click")) {
                clicks++;
                if (mode.equals("click")) throw primary;
                if (!mode.equals("no-event")) listener.accept(download);
            }
            return null;
        });
        return actions.download(page, new AtfLocator(locator, "#invoice"), "the owned download event is received", (budget, ready) -> {
            readiness++;
            if (mode.equals("readiness") || mode.equals("same") || mode.equals("distinct")) throw (RuntimeException) primary;
        });
    }
    public Throwable primary() { return primary; }
    public Throwable cleanup() { return cleanup; }
    public int clicks() { return clicks; }
    public int removals() { return removals; }
    public int readiness() { return readiness; }
    public boolean detached() { return listener == null; }
    public java.util.List<atf.core.ui.AtfUiActionRecord> records() { return actions.snapshot(); }
    @SuppressWarnings("unchecked") private static Consumer<Download> callback(Object[] args) { return (Consumer<Download>) args[0]; }
    @FunctionalInterface private interface Invocation { Object invoke(String name, Object[] args) throws Throwable; }
    private static <T> T proxy(Class<T> type, Invocation call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (instance, method, args) -> call.invoke(method.getName(), args)));
    }
}
