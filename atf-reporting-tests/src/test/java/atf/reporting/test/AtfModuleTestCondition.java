package atf.reporting.test;

import atf.core.config.module.AtfModule;
import atf.core.config.module.AtfModuleConfigurationLoader;
import atf.core.config.module.AtfModuleSet;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Arrays;

/** Fail-closed condition for configurable reporting composition roots. */
public final class AtfModuleTestCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        AtfModuleTest declaration = context.getElement()
                .map(element -> element.getAnnotation(AtfModuleTest.class))
                .orElse(null);
        if (declaration == null) {
            declaration = context.getTestClass()
                    .map(type -> type.getAnnotation(AtfModuleTest.class))
                    .orElse(null);
        }
        if (declaration == null) return ConditionEvaluationResult.enabled("No ATF module gate declared");

        AtfModuleSet configured = AtfModuleConfigurationLoader.load();
        var missing = Arrays.stream(declaration.requires())
                .filter(module -> !configured.contains(module))
                .map(AtfModule::key)
                .toList();
        if (!missing.isEmpty()) {
            return ConditionEvaluationResult.disabled(
                    "ATF test inhibited; missing configured module(s): " + missing);
        }
        return ConditionEvaluationResult.enabled("Required ATF modules are configured");
    }
}
