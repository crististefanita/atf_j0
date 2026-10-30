package atf.ui.playwright.recovery;

import atf.testkit.AtfComponentTest;
import atf.ui.recovery.*;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.time.Clock;
import java.util.*;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class UiRecoveryObserverLifecycleComponentTest {
    @Test void repeatedErrorCannotInterruptTheRemainingRemovals() {
        var fixture = new Events();
        var shared = new AssertionError("shared cleanup");
        fixture.cleanup.put("offRequest", shared); fixture.cleanup.put("offResponse", shared);
        var observer = fixture.open();
        assertSame(shared, assertThrows(AssertionError.class, observer::close));
        assertEquals(Events.OFF, fixture.removed);
        observer.close(); assertEquals(4, fixture.removed.size());
        assertEquals(0, shared.getSuppressed().length);
        assertTrue(observer.signal().isEmpty());
    }

    @Test void distinctFailuresRemainOrderedAndDoNotPreventCleanup() {
        var fixture = new Events();
        Events.OFF.forEach(name -> fixture.cleanup.put(name, new IllegalStateException(name)));
        var observer = fixture.open();
        var primary = assertThrows(IllegalStateException.class, observer::close);
        assertSame(fixture.cleanup.get("offRequest"), primary);
        assertArrayEquals(Events.OFF.stream().skip(1).map(fixture.cleanup::get).toArray(), primary.getSuppressed());
        assertEquals(Events.OFF, fixture.removed);
    }

    @Test void failedAcquisitionRetainsItsIdentityEvenWhenCleanupThrowsTheSameObject() {
        var fixture = new Events();
        fixture.attachFailure = new IllegalStateException("attachment");
        fixture.cleanup.put("offRequest", fixture.attachFailure);
        var later = new AssertionError("later cleanup"); fixture.cleanup.put("offResponse", later);
        assertSame(fixture.attachFailure, assertThrows(IllegalStateException.class, fixture::open));
        assertArrayEquals(new Throwable[]{later}, fixture.attachFailure.getSuppressed());
        assertEquals(Events.OFF, fixture.removed);
    }

    @Test void callbacksAreInertDuringPartialAttachmentAndAfterClose() {
        var fixture = new Events(); fixture.fireDuringAttach = true;
        var observer = fixture.open();
        assertEquals(0, fixture.reads); assertTrue(observer.failure().isEmpty());
        observer.close(); fixture.requestCallback().accept(fixture.unavailableRequest());
        @SuppressWarnings("unchecked") var response = (Consumer<Response>) fixture.callbacks.get("onResponse");
        response.accept(fake(Response.class, (name, args) -> { throw new AssertionError("closed response accessed"); }));
        @SuppressWarnings("unchecked") var failed = (Consumer<Request>) fixture.callbacks.get("onRequestFailed");
        failed.accept(fixture.unavailableRequest());
        assertEquals(0, fixture.reads); assertTrue(observer.signal().isEmpty());
    }

    interface Invocation { Object call(String method, Object[] args); }
    @SuppressWarnings("unchecked") static <T> T fake(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> invocation.call(method.getName(), args));
    }
    static final class Events {
        static final List<String> OFF = List.of("offRequest", "offResponse", "offRequestFailed", "offFrameNavigated");
        final Map<String, Object> callbacks = new HashMap<>();
        final Map<String, Throwable> cleanup = new HashMap<>();
        final List<String> removed = new ArrayList<>();
        final Frame frame = fake(Frame.class, (name,args) -> null);
        RuntimeException attachFailure; boolean fireDuringAttach; int reads;
        final Page page = fake(Page.class, (name,args) -> {
            if (name.equals("mainFrame")) return frame;
            if (name.equals("isClosed")) return false;
            if (name.startsWith("on")) {
                callbacks.put(name,args[0]);
                if (fireDuringAttach && name.equals("onResponse")) requestCallback().accept(unavailableRequest());
                if (name.equals("onFrameNavigated") && attachFailure != null) throw attachFailure;
            }
            if (name.startsWith("off")) {
                removed.add(name); var error = cleanup.get(name);
                if (error instanceof RuntimeException runtime) throw runtime;
                if (error instanceof Error fatal) throw fatal;
            }
            return null;
        });
        @SuppressWarnings("unchecked") Consumer<Request> requestCallback() { return (Consumer<Request>) callbacks.get("onRequest"); }
        Request unavailableRequest() { return fake(Request.class, (name,args) -> { reads++; throw new IllegalStateException("unavailable"); }); }
        UiRecoveryObserver open() {
            var origin = URI.create("http://localhost/");
            return new UiRecoveryObserver(page, new UiRecoveryOperation("catalog", UiRecoveryOperation.Safety.READ_ONLY,
                    origin, origin.resolve("products"), "GET"),
                    new UiRecoverySignal.Stamp(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 0), Clock.systemUTC(), true);
        }
    }
}
