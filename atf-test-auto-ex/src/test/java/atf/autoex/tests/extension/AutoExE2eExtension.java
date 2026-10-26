package atf.autoex.tests.extension;

import atf.testkit.lifecycle.AtfE2eActivationPolicy;
import atf.autoex.config.AutoExConfigurationLoader;
import atf.autoex.runtime.AutoExE2eRun;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/** Opt-in site boundary. It opens no browser for a normal reactor build. */
public final class AutoExE2eExtension implements ExecutionCondition,
        BeforeEachCallback, AfterEachCallback, ParameterResolver {
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(AutoExE2eExtension.class);
    private static final String RUN_KEY = "auto-ex-e2e-run";
    private static final String ENABLE_PROPERTY = "atf.autoex.e2e.enabled";
    private static final String INTELLIJ_RUNNER_PROPERTY = "idea.test.cyclic.buffer.size";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        String override = System.getProperty(ENABLE_PROPERTY);
        boolean yamlEnabled = override == null && AutoExConfigurationLoader.load().e2eEnabled();
        boolean intellijDetected = override == null
                && System.getProperty(INTELLIJ_RUNNER_PROPERTY) != null;
        var decision = AtfE2eActivationPolicy.evaluate(ENABLE_PROPERTY, override, yamlEnabled,
                yamlEnabled ? "AutoEx YAML configuration" : null, intellijDetected);
        return decision.enabled()
                ? ConditionEvaluationResult.enabled(decision.reason())
                : ConditionEvaluationResult.disabled(decision.reason());
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        context.getStore(NAMESPACE).put(RUN_KEY, AutoExRunFactory.open(context));
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        AutoExE2eRun run = context.getStore(NAMESPACE).remove(RUN_KEY, AutoExE2eRun.class);
        if (run != null) run.finish(context.getExecutionException().orElse(null));
    }

    @Override
    public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
        return parameter.getParameter().getType() == AutoExE2eRun.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
        AutoExE2eRun run = context.getStore(NAMESPACE).get(RUN_KEY, AutoExE2eRun.class);
        if (run == null) throw new ParameterResolutionException("Automation Exercise E2E run is not open");
        return run;
    }
}
