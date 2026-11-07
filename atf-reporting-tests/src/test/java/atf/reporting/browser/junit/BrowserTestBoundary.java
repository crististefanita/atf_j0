package atf.reporting.browser.junit;

import atf.reporting.browser.lifecycle.BrowserProbeScope;
import java.lang.reflect.Method;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.*;

/** One worker preserves the original class lifecycle and the host's actual selected methods/cases. */
public final class BrowserTestBoundary implements InvocationInterceptor, BeforeAllCallback {
    private static final ExtensionContext.Namespace STORE = ExtensionContext.Namespace.create(BrowserTestBoundary.class);

    @Override public void beforeAll(ExtensionContext context) throws Exception {
        if (!BrowserProbeScope.active()) context.getStore(STORE).put("run", BrowserClassRun.execute(context.getRequiredTestClass()));
    }
    @Override public void interceptBeforeAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> call, ExtensionContext context) throws Throwable { lifecycle(invocation); }
    @Override public void interceptBeforeEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> call, ExtensionContext context) throws Throwable { lifecycle(invocation); }
    @Override public void interceptAfterEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> call, ExtensionContext context) throws Throwable { lifecycle(invocation); }
    @Override public void interceptAfterAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> call, ExtensionContext context) throws Throwable { lifecycle(invocation); }
    private static void lifecycle(Invocation<Void> invocation) throws Throwable { if (BrowserProbeScope.active()) guarded(invocation); else invocation.skip(); }

    @Override public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> call, ExtensionContext context) throws Throwable { execute(invocation, context); }
    @Override public void interceptTestTemplateMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> call, ExtensionContext context) throws Throwable { execute(invocation, context); }
    @Override public void interceptDynamicTest(Invocation<Void> invocation, DynamicTestInvocationContext call, ExtensionContext context) throws Throwable { execute(invocation, context); }
    private static void execute(Invocation<Void> invocation, ExtensionContext context) throws Throwable {
        if (BrowserProbeScope.active()) { guarded(invocation); return; }
        invocation.skip();
        run(context).assertCase(BrowserSelection.normalize(context.getUniqueId()));
    }

    @Override public <T> T interceptTestFactoryMethod(Invocation<T> invocation, ReflectiveInvocationContext<Method> call, ExtensionContext context) throws Throwable {
        if (BrowserProbeScope.active()) return guarded(invocation);
        invocation.skip();
        if (!Stream.class.isAssignableFrom(call.getExecutable().getReturnType())) throw new IllegalStateException("Unsupported browser factory result: " + call.getExecutable());
        @SuppressWarnings("unchecked") T result = (T) run(context).dynamicChildren(BrowserSelection.normalize(context.getUniqueId()));
        return result;
    }

    private static <T> T guarded(Invocation<T> invocation) throws Throwable {
        try {
            T result = invocation.proceed();
            BrowserProbeScope.bodyCompleted();
            return result;
        } catch (Throwable original) { BrowserProbeScope.escapedFailure(original); throw original; }
    }

    private static BrowserClassRun run(ExtensionContext context) {
        for (ExtensionContext current = context; current != null; current = current.getParent().orElse(null)) {
            BrowserClassRun run = current.getStore(STORE).get("run", BrowserClassRun.class);
            if (run != null) return run;
        }
        throw new IllegalStateException("Browser class has no completed worker result");
    }
}
