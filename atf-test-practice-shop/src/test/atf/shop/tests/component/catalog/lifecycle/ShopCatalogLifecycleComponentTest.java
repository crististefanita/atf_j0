package atf.shop.tests.component.catalog.lifecycle;

import atf.shop.support.catalog.ShopCatalogReadiness;
import atf.shop.tests.support.ShopComponentTest;
import atf.ui.playwright.PlaywrightActionSequence;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.*;
import java.util.function.Consumer;
import static atf.testkit.assertions.AuditedAssertions.*;

@ShopComponentTest
class ShopCatalogLifecycleComponentTest {
    @Test void allDistinctCleanupFailuresSurviveInTheirOriginalOrder() {
        var fixture = new Events();
        Events.OFF.forEach(name -> fixture.cleanup.put(name, new IllegalStateException(name)));
        var catalog = fixture.open();
        var primary = assertThrows(IllegalStateException.class, catalog::close);
        assertSame(fixture.cleanup.get("offRequest"), primary);
        assertEquals(List.of(fixture.cleanup.get("offResponse"), fixture.cleanup.get("offRequestFailed"),
                fixture.cleanup.get("offFrameNavigated")), Arrays.asList(primary.getSuppressed()));
        assertEquals(Events.OFF, fixture.removed);
        catalog.close(); assertEquals(4, fixture.removed.size());
    }
    @Test void identicalErrorsNeverCauseSelfSuppressionOrSkipRemoval() {
        var fixture = new Events(); var shared = new AssertionError("shared");
        fixture.cleanup.put("offRequest", shared); fixture.cleanup.put("offResponse", shared);
        var catalog = fixture.open();
        assertSame(shared, assertThrows(AssertionError.class, catalog::close));
        assertEquals(Events.OFF, fixture.removed); assertEquals(0, shared.getSuppressed().length);
    }
    @ParameterizedTest @ValueSource(ints={1,2,3,4})
    void eachPartialAcquisitionKeepsItsPrimaryAndEveryCleanupFailure(int failAt) {
        var fixture = new Events(); fixture.failAt=failAt;
        Events.OFF.forEach(name -> fixture.cleanup.put(name, new IllegalStateException(name)));
        fixture.cleanup.put("offRequest", fixture.primary);
        assertSame(fixture.primary, assertThrows(IllegalStateException.class, fixture::open));
        assertEquals(Events.OFF, fixture.removed);
        assertEquals(3, fixture.primary.getSuppressed().length);
    }
    @Test void retainedCallbacksNeverReadRequestsWhileAttachingOrAfterClose() {
        var fixture = new Events(); fixture.fireDuringAttach=true;
        var catalog = fixture.open(); assertEquals(0, fixture.reads);
        catalog.close(); fixture.callback().accept(fixture.unavailable());
        assertEquals(0, fixture.reads);
    }
    interface Invocation { Object call(String method,Object[] args); }
    @SuppressWarnings("unchecked") static <T> T fake(Class<T> type,Invocation call) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},
                (proxy,method,args)->call.call(method.getName(),args));
    }
    static final class Events {
        static final List<String> OFF=List.of("offRequest","offResponse","offRequestFailed","offFrameNavigated");
        final List<String> removed=new ArrayList<>(); final Map<String,Throwable> cleanup=new HashMap<>();
        final Map<String,Object> callbacks=new HashMap<>();
        final RuntimeException primary=new IllegalStateException("attachment");
        int failAt, attachments, reads; boolean fireDuringAttach;
        final Page page=fake(Page.class,(name,args)->{
            if(name.startsWith("on")) {
                callbacks.put(name,args[0]);
                if(fireDuringAttach && name.equals("onResponse")) callback().accept(unavailable());
                if(++attachments==failAt) throw primary;
            }
            if(name.startsWith("off")) {
                removed.add(name); var failure=cleanup.get(name);
                if(failure instanceof RuntimeException runtime) throw runtime;
                if(failure instanceof Error error) throw error;
            }
            return null;
        });
        @SuppressWarnings("unchecked") Consumer<Request> callback(){return (Consumer<Request>)callbacks.get("onRequest");}
        Request unavailable(){return fake(Request.class,(name,args)->{reads++;throw new IllegalStateException("request unavailable");});}
        ShopCatalogReadiness open(){
            var home=URI.create("http://localhost/");
            return new ShopCatalogReadiness(page,home,home.resolve("products"),new PlaywrightActionSequence());
        }
    }
}
