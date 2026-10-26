package atf.shop.tests.api.reporting;

import atf.api.testkit.AtfUnitTest;
import atf.api.testkit.channel.AtfTestChannel;
import atf.api.testkit.channel.TestChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** An invalid channel must not create an API report during either JUnit callback. */
@AtfUnitTest
class ShopApiCallAuditPreflightTest {
    @AtfTestChannel(TestChannel.UI)
    private static class UnsupportedOwner { }

    @Test
    void rejectedChannelNeverOpensTheSuiteReportInAfterEach() {
        AtomicInteger storeCalls = new AtomicInteger();
        AtomicInteger rootCalls = new AtomicInteger();
        ExtensionContext context = context(storeCalls, rootCalls);
        var extension = new ShopApiCallAuditTestSupport();

        assertThrows(IllegalStateException.class, () -> extension.beforeEach(context));
        extension.afterEach(context);

        assertEquals(1, storeCalls.get(), "afterEach may inspect its own unstarted method store");
        assertEquals(0, rootCalls.get(), "the suite report must not be opened after preflight failure");
    }

    private static ExtensionContext context(AtomicInteger storeCalls, AtomicInteger rootCalls) {
        var store = (ExtensionContext.Store) Proxy.newProxyInstance(
                ExtensionContext.Store.class.getClassLoader(),
                new Class<?>[]{ExtensionContext.Store.class},
                (proxy, method, arguments) -> {
                    if ("remove".equals(method.getName())) return null;
                    throw new AssertionError("Unexpected store call: " + method.getName());
                });
        return (ExtensionContext) Proxy.newProxyInstance(
                ExtensionContext.class.getClassLoader(),
                new Class<?>[]{ExtensionContext.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getRequiredTestClass" -> UnsupportedOwner.class;
                    case "getStore" -> {
                        storeCalls.incrementAndGet();
                        yield store;
                    }
                    case "getRoot" -> {
                        rootCalls.incrementAndGet();
                        throw new AssertionError("Preflight failure must not create a root report");
                    }
                    default -> throw new AssertionError("Unexpected context call: " + method.getName());
                });
    }
}
