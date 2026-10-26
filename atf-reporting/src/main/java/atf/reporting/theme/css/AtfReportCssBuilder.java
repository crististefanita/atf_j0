package atf.reporting.theme.css;

import atf.reporting.theme.tokens.AtfReportCssProperty;
import atf.reporting.theme.tokens.AtfReportCssSelectors;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Small deterministic CSS builder for semantic report components.
 *
 * <p>Selectors and token values remain typed at their owners; this builder
 * owns only serialization. It is intentionally strict so a component test can
 * validate a rule model before the complete theme is composed.</p>
 */
public final class AtfReportCssBuilder {
    private final List<Rule> rules = new ArrayList<>();

    public RuleBuilder rule(String selector) {
        RuleBuilder builder = new RuleBuilder(selector);
        rules.add(builder.rule);
        return builder;
    }

    public RuleBuilder ruleTyped(AtfReportCssSelectors.Surface surface) {
        return rule(surface.selector());
    }

    public RuleBuilder ruleTyped(AtfReportCssSelectors.Component component) {
        return rule(component.selector());
    }

    public RuleBuilder ruleTyped(AtfReportCssSelectors.Fallback fallback) {
        return rule(fallback.selector());
    }

    public String build() {
        return rules.stream().map(Rule::render).collect(Collectors.joining());
    }

    public static final class RuleBuilder {
        private final Rule rule;

        private RuleBuilder(String selector) {
            this.rule = new Rule(selector);
        }

        public RuleBuilder declaration(String property, String value) {
            return declaration(property, value, false);
        }

        public RuleBuilder declaration(AtfReportCssProperty property, String value) {
            return declaration(property.value(), value, false);
        }

        public RuleBuilder important(String property, String value) {
            return declaration(property, value, true);
        }

        public RuleBuilder important(AtfReportCssProperty property, String value) {
            return declaration(property.value(), value, true);
        }

        private RuleBuilder declaration(String property, String value, boolean important) {
            rule.declarations.add(new Declaration(property, value, important));
            return this;
        }
    }

    public record Declaration(String property, String value, boolean important) {
        public Declaration {
            requireText(property, "property");
            requireText(value, "value");
        }

        String render() {
            return property + ":" + value + (important ? "!important" : "");
        }
    }

    private static final class Rule {
        private final String selector;
        private final List<Declaration> declarations = new ArrayList<>();

        private Rule(String selector) {
            this.selector = requireText(selector, "selector");
        }

        private String render() {
            return selector + "{" + declarations.stream()
                    .map(Declaration::render)
                    .collect(Collectors.joining(";")) + "}";
        }
    }

    private static String requireText(String value, String field) {
        if (Objects.requireNonNull(value, field).isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
