package atf.ui.tests.extent.support.lifecycle;

import atf.reporting.browser.lifecycle.BrowserLease;
import atf.reporting.browser.lifecycle.BrowserProbeScope;
import com.microsoft.playwright.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.lang.ref.WeakReference;
import java.util.function.Supplier;

/** SDK values remain concrete; their acquisition, ownership and disposal have explicit phase boundaries. */
public final class BrowserResources {
    private record Owned(Object value, Object parent, AutoCloseable disposer) { }
    private static final Map<Object, Owned> OWNED = new IdentityHashMap<>();
    private static final Map<Object, Object> ALIASES = new IdentityHashMap<>();
    private static final ArrayList<WeakReference<Object>> CLOSED = new ArrayList<>();
    private static final ArrayList<Object> ORDER = new ArrayList<>();

    private BrowserResources() { }

    public static <T> T create(Supplier<T> factory) { return acquire(() -> register(factory.get(), null)); }

    private static <T> T acquire(Supplier<T> factory) {
        try (var phase = BrowserProbeScope.setup()) { return factory.get(); }
        catch (RuntimeException | Error original) { recordFailure(original); throw original; }
    }

    public static void recordFailure(Throwable original) {
        if (BrowserProbeScope.active()) BrowserProbeScope.recordFailure(original);
    }

    public static <T> BrowserLease<T> open(Supplier<T> factory) { return new BrowserLease<>(create(factory), BrowserResources::close); }

    public static Browser launch(Playwright playwright) { return launch(playwright, new BrowserType.LaunchOptions()); }
    public static Browser launch(Playwright playwright, BrowserType.LaunchOptions options) {
        return acquire(() -> register(playwright.chromium().launch(options), playwright));
    }

    public static BrowserContext context(Browser browser, Browser.NewContextOptions options) {
        return acquire(() -> register(browser.newContext(options), browser));
    }
    public static BrowserContext context(Browser browser) { return context(browser, new Browser.NewContextOptions()); }

    public static Page newPage(Browser browser) { return newPage(browser, new Browser.NewContextOptions()); }
    public static Page newPage(Browser browser, Browser.NewContextOptions options) {
        try (var phase = BrowserProbeScope.setup()) {
            BrowserContext context = context(browser, options);
            try {
                Page page = register(context.newPage(), context);
                ALIASES.put(page, context);
                return page;
            } catch (RuntimeException | Error original) {
                BrowserProbeScope.recordFailure(original);
                try { close(context); } catch (RuntimeException | Error cleanup) { if (cleanup != original) original.addSuppressed(cleanup); }
                throw original;
            }
        }
    }

    public static Page newPage(BrowserContext context) {
        // An explicit context already owns its pages. Its close operation disposes them together.
        return acquire(context::newPage);
    }

    private static <T> T register(T value, Object parent) {
        if (value instanceof AutoCloseable closeable && !OWNED.containsKey(value) && !ALIASES.containsKey(value)) {
            OWNED.put(value, new Owned(value, parent, closeable)); ORDER.add(value);
        }
        return value;
    }

    public static void close(Object value) {
        if (value == null) return;
        Object resolved = ALIASES.getOrDefault(value, value);
        if (!firstClose(resolved)) return;
        Owned owner = OWNED.remove(resolved);
        ORDER.removeIf(item -> item == resolved);
        ALIASES.entrySet().removeIf(entry -> entry.getValue() == resolved);
        if (owner == null && !(resolved instanceof AutoCloseable)) throw new IllegalArgumentException("Not a closeable browser resource");
        boolean sdk = owner != null || resolved instanceof Page || resolved instanceof Browser || resolved instanceof BrowserContext || resolved instanceof Playwright;
        dispose(resolved, owner, sdk);
    }

    private static void dispose(Object value, Owned owner, boolean sdk) {
        Throwable failure = null;
        var children = new ArrayList<Object>();
        for (var item : OWNED.values()) if (item.parent() == value) children.add(item.value());
        for (Object child : children) try { close(child); } catch (RuntimeException | Error error) { failure = combine(failure, error); }
        try (var phase = sdk ? BrowserProbeScope.cleanup() : null) {
            if (owner == null) ((AutoCloseable) value).close(); else owner.disposer().close();
        }
        catch (Throwable error) {
            BrowserProbeScope.recordCleanupFailure(error);
            failure = combine(failure, error);
        }
        if (failure != null) rethrow(failure);
    }

    public static void dispose(JSHandle handle) {
        try (var phase = BrowserProbeScope.cleanup()) { handle.dispose(); }
    }

    public static void closeAll(Object... values) {
        Throwable failure = null;
        for (Object value : values) try { close(value); } catch (RuntimeException | Error error) { failure = combine(failure, error); }
        if (failure != null) rethrow(failure);
    }

    public static void finish(Runnable verification, Object... values) {
        Throwable failure = null;
        try { verification.run(); } catch (RuntimeException | Error original) { failure = original; BrowserProbeScope.recordFailure(original); }
        try { closeAll(values); } catch (RuntimeException | Error cleanup) { failure = combine(failure, cleanup); }
        if (failure != null) rethrow(failure);
    }

    public static void closeRemaining() {
        var remaining = new ArrayList<>(ORDER); Collections.reverse(remaining);
        Throwable failure = null;
        for (Object value : remaining) if (OWNED.containsKey(value))
            try { close(value); } catch (RuntimeException | Error error) { failure = combine(failure, error); }
        if (failure != null) rethrow(failure);
    }

    private static Throwable combine(Throwable original, Throwable next) {
        if (original == null) return next;
        if (original != next) original.addSuppressed(next);
        return original;
    }

    private static boolean firstClose(Object value) {
        boolean known = false;
        for (var iterator = CLOSED.iterator(); iterator.hasNext();) {
            Object previous = iterator.next().get();
            if (previous == null) iterator.remove();
            else if (previous == value) known = true;
        }
        if (!known) CLOSED.add(new WeakReference<>(value));
        return !known;
    }
    private static void rethrow(Throwable error) {
        if (error instanceof RuntimeException runtime) throw runtime;
        if (error instanceof Error fatal) throw fatal;
        throw new IllegalStateException("Browser resource disposal failed", error);
    }
}
