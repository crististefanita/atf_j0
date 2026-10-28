package atf.testkit.assertions;

import atf.testkit.lifecycle.FailureAwareCleanup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;
import java.lang.reflect.Method;
import java.util.Objects;

/** Composable JUnit adapter; consumers supply publication without exposing report types to Kit. */
public final class ComponentAuditExtension implements BeforeEachCallback, AfterEachCallback,
        ParameterResolver, InvocationInterceptor {
    public interface Publication extends AutoCloseable {
        AssertionValueDisplay display();
        void publish(ComponentAssertionAudit.Result result) throws Exception;
        @Override void close() throws Exception;
    }
    @FunctionalInterface public interface Factory { Publication open(ExtensionContext context) throws Exception; }
    private record State(ComponentAssertionAudit audit, Publication publication) { }
    private final ExtensionContext.Namespace namespace;
    private final Factory factory;

    public ComponentAuditExtension(Object owner, Factory factory) {
        namespace = ExtensionContext.Namespace.create(Objects.requireNonNull(owner));
        this.factory = Objects.requireNonNull(factory);
    }

    @Override public void beforeEach(ExtensionContext context) throws Exception {
        Publication publication = factory.open(context);
        try {
            var audit = new ComponentAssertionAudit(publication.display());
            context.getStore(namespace).put(State.class, new State(audit, publication));
        } catch (RuntimeException | Error failure) {
            FailureAwareCleanup.closeAfterFailure(failure, publication);
            throw failure;
        }
    }

    @Override public void afterEach(ExtensionContext context) throws Exception {
        State state = context.getStore(namespace).remove(State.class, State.class);
        if (state == null) return;
        Throwable primary = context.getExecutionException().orElse(null);
        var result = state.audit().finish(primary);
        Throwable publicationFailure = FailureAwareCleanup.runAll(null,
                () -> state.publication().publish(result), state.publication()::close);
        if (publicationFailure != null) {
            if (result.outcome() == ComponentAssertionAudit.Outcome.ABORTED) {
                throwFailure(FailureAwareCleanup.combine(publicationFailure, primary));
            }
            Throwable failure = FailureAwareCleanup.combine(result.failure(), publicationFailure);
            if (primary == null || result.propagate()) throwFailure(failure);
        }
        if (result.propagate()) throwFailure(result.failure());
    }

    @Override public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
        return parameter.getParameter().getType() == RecordedAssertions.class;
    }

    @Override public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
        var executable = parameter.getDeclaringExecutable();
        var phase = AnnotationSupport.isAnnotated(executable, BeforeEach.class)
                ? ComponentAssertionAudit.Phase.SETUP : AnnotationSupport.isAnnotated(executable, AfterEach.class)
                ? ComponentAssertionAudit.Phase.TEARDOWN : ComponentAssertionAudit.Phase.TEST_BODY;
        return state(context).audit().recorder(phase);
    }

    @Override public void interceptBeforeEachMethod(Invocation<Void> call, ReflectiveInvocationContext<Method> method,
                                                   ExtensionContext context) throws Throwable {
        invoke(call, method, context, ComponentAssertionAudit.Phase.SETUP);
    }
    @Override public void interceptTestMethod(Invocation<Void> call, ReflectiveInvocationContext<Method> method,
                                             ExtensionContext context) throws Throwable {
        invoke(call, method, context, ComponentAssertionAudit.Phase.TEST_BODY);
    }
    @Override public void interceptTestTemplateMethod(Invocation<Void> call, ReflectiveInvocationContext<Method> method,
                                                     ExtensionContext context) throws Throwable {
        invoke(call, method, context, ComponentAssertionAudit.Phase.TEST_BODY);
    }
    @Override public void interceptAfterEachMethod(Invocation<Void> call, ReflectiveInvocationContext<Method> method,
                                                  ExtensionContext context) throws Throwable {
        invoke(call, method, context, ComponentAssertionAudit.Phase.TEARDOWN);
    }

    private void invoke(Invocation<Void> call, ReflectiveInvocationContext<Method> method,
                        ExtensionContext context, ComponentAssertionAudit.Phase phase) throws Throwable {
        var audit = state(context).audit();
        audit.start(phase);
        try (var scope = AuditedAssertionScope.open(method.getExecutable().getDeclaringClass().getName(),
                method.getExecutable().getName(), audit.recorder(phase))) {
            call.proceed();
        } catch (Throwable failure) {
            audit.failed(phase, failure);
            throw failure;
        }
    }

    private State state(ExtensionContext context) {
        var state = context.getStore(namespace).get(State.class, State.class);
        if (state == null) throw new IllegalStateException("Component audit requires a per-method JUnit invocation");
        return state;
    }
    private static void throwFailure(Throwable failure) throws Exception {
        if (failure instanceof Exception exception) throw exception;
        throw (Error) failure;
    }
}
