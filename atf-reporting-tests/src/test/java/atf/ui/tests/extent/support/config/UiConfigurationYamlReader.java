package atf.ui.tests.extent.support.config;

import atf.ui.config.UiBrowserConfiguration;
import atf.ui.config.UiScreenValidationConfiguration;
import atf.ui.config.UiRetryOperation;
import atf.ui.config.UiRetryPolicy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.EnumSet;
import java.util.stream.StreamSupport;

/**
 * Parses and validates the {@code ui} section of one YAML file.
 *
 * <p>Process-boundary discovery and system-property precedence belong to
 * {@link AtfUiConfigurationLoader}; this class owns only document parsing,
 * typed conversion and report-path containment.</p>
 */
final class UiConfigurationYamlReader {
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private UiConfigurationYamlReader() { }

    static UiConfiguration read(Path config) {
        try {
            JsonNode root = YAML.readTree(config.toFile());
            JsonNode ui = root.path("ui");
            Path base = config.getParent() == null
                    ? Path.of(".").toAbsolutePath().normalize()
                    : config.getParent();

            UiBrowserConfiguration browser = browser(ui);
            Path reportDirectory = reportDirectory(ui, base);
            List<String> scenarioFamilies = scenarioFamilies(ui);
            UiScreenValidationConfiguration screenValidation = screenValidation(ui);
            Optional<Path> auditInputReport = auditInputReport(ui, base);
            Map<String, UiAuditSuiteConfiguration> auditSuites = auditSuites(ui);
            JsonNode report = ui.path("report");

            return new UiConfiguration(browser, reportDirectory,
                    text(report, "filePrefix", "ui-atf-extent-report_"), scenarioFamilies,
                    screenValidation, auditInputReport, auditSuites);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read ATF UI configuration: " + config, error);
        }
    }

    private static UiBrowserConfiguration browser(JsonNode ui) {
        JsonNode viewport = ui.path("viewport");
        return new UiBrowserConfiguration(
                URI.create(text(ui, "baseUrl", "about:blank")),
                text(ui, "browser", "chromium"),
                bool(ui, "headless", true),
                duration(ui, "timeout", Duration.ofSeconds(10)),
                duration(ui, "navigationTimeout", Duration.ofSeconds(15)),
                integer(viewport, "width", 1440, "ui.viewport.width"),
                integer(viewport, "height", 900, "ui.viewport.height"),
                integer(ui, "zoomPercent", UiBrowserConfiguration.DEFAULT_ZOOM_PERCENT,
                        "ui.zoomPercent"),
                bool(ui, "screenshotOnFailure", true),
                retryPolicy(ui));
    }

    private static UiRetryPolicy retryPolicy(JsonNode ui) {
        JsonNode retry = ui.path("retry");
        List<UiRetryOperation> operations = new java.util.ArrayList<>();
        JsonNode configured = retry.path("eligibleOperations");
        if (configured.isArray()) {
            StreamSupport.stream(configured.spliterator(), false).forEach(node -> {
                if (!node.isTextual() || node.asText().isBlank()) {
                    throw new IllegalStateException(
                            "ATF UI retry operations must be text: ui.retry.eligibleOperations");
                }
                operations.add(UiRetryOperation.from(node.asText()));
            });
        }
        return new UiRetryPolicy(
                integer(retry, "maxAttempts", UiRetryPolicy.DEFAULT_MAX_ATTEMPTS,
                        "ui.retry.maxAttempts"),
                duration(retry, "backoff", UiRetryPolicy.DEFAULT_BACKOFF),
                operations.isEmpty() ? EnumSet.noneOf(UiRetryOperation.class) : EnumSet.copyOf(operations),
                text(retry, "reason", UiRetryPolicy.DEFAULT_REASON));
    }

    private static Path reportDirectory(JsonNode ui, Path base) {
        JsonNode report = ui.path("report");
        Path directory = Path.of(text(report, "directory", "artifacts/reports/ui"));
        Path normalizedBase = base.toAbsolutePath().normalize();
        Path resolved = (directory.isAbsolute() ? directory : base.resolve(directory))
                .toAbsolutePath().normalize();
        if (!resolved.startsWith(normalizedBase)) {
            throw new IllegalStateException("ui.report.directory escapes the configuration directory");
        }
        return resolved;
    }

    private static List<String> scenarioFamilies(JsonNode ui) {
        JsonNode families = ui.path("apiSetup").path("scenarioFamilies");
        if (!families.isArray()) return UiConfiguration.defaultScenarioFamilies();
        return StreamSupport.stream(families.spliterator(), false)
                .map(node -> {
                    if (!node.isTextual() || node.asText().isBlank()) {
                        throw new IllegalStateException(
                                "ATF UI API scenario family must be text: ui.apiSetup.scenarioFamilies");
                    }
                    return node.asText().trim().toLowerCase();
                }).toList();
    }

    private static UiScreenValidationConfiguration screenValidation(JsonNode ui) {
        JsonNode policy = ui.path("screenValidation");
        return new UiScreenValidationConfiguration(
                bool(policy, "enabled", true),
                bool(policy, "fullPage", false),
                duration(policy, "screenshotTimeout",
                        UiScreenValidationConfiguration.DEFAULT_SCREENSHOT_TIMEOUT));
    }

    private static Optional<Path> auditInputReport(JsonNode ui, Path base) {
        JsonNode audit = ui.path("audit");
        JsonNode value = audit.path("inputReport");
        if (value.isMissingNode() || value.isNull()) return Optional.empty();
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new IllegalStateException("ATF UI value must be text: ui.audit.inputReport");
        }
        Path path = Path.of(value.asText().trim());
        Path normalizedBase = base.toAbsolutePath().normalize();
        Path resolved = (path.isAbsolute() ? path : base.resolve(path))
                .toAbsolutePath().normalize();
        if (!path.isAbsolute() && !resolved.startsWith(normalizedBase)) {
            throw new IllegalStateException("ui.audit.inputReport escapes the configuration directory");
        }
        return Optional.of(resolved);
    }

    private static Map<String, UiAuditSuiteConfiguration> auditSuites(JsonNode ui) {
        JsonNode suites = ui.path("audit").path("suites");
        if (!suites.isObject()) return Map.of();
        Map<String, UiAuditSuiteConfiguration> result = new LinkedHashMap<>();
        suites.fields().forEachRemaining(entry -> {
            String id = entry.getKey().trim().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
            JsonNode suite = entry.getValue();
            result.put(id, new UiAuditSuiteConfiguration(id,
                    strings(suite, "scenarioFamilies", "families"),
                    strings(suite, "tabs"), strings(suite, "validators"),
                    text(suite, "depth", "deep"), text(suite, "source", "yaml")));
        });
        return result;
    }

    private static List<String> strings(JsonNode parent, String... names) {
        JsonNode value = null;
        for (String name : names) {
            JsonNode candidate = parent.path(name);
            if (candidate.isArray()) { value = candidate; break; }
        }
        if (value == null) return List.of();
        return StreamSupport.stream(value.spliterator(), false).map(node -> {
            if (!node.isTextual() || node.asText().isBlank()) {
                throw new IllegalStateException("ATF UI suite list values must be text");
            }
            return node.asText().trim();
        }).toList();
    }

    private static String text(JsonNode parent, String name, String fallback) {
        JsonNode value = parent.path(name);
        if (value.isMissingNode() || value.isNull()) return fallback;
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new IllegalStateException("ATF UI value must be text: ui." + name);
        }
        return value.asText().trim();
    }

    private static boolean bool(JsonNode parent, String name, boolean fallback) {
        JsonNode value = parent.path(name);
        if (value.isMissingNode() || value.isNull()) return fallback;
        if (!value.isBoolean()) {
            throw new IllegalStateException("ATF UI value must be boolean: ui." + name);
        }
        return value.booleanValue();
    }

    private static int integer(JsonNode parent, String name, int fallback, String key) {
        JsonNode value = parent.path(name);
        if (value.isMissingNode() || value.isNull()) return fallback;
        if (value.canConvertToInt() && value.intValue() > 0) return value.intValue();
        throw new IllegalStateException("ATF UI value must be a positive integer: " + key);
    }

    private static Duration duration(JsonNode parent, String name, Duration fallback) {
        String raw = text(parent, name, fallback.toString());
        try {
            return Duration.parse(raw);
        } catch (RuntimeException error) {
            throw new IllegalStateException("ATF UI duration is invalid: ui." + name, error);
        }
    }
}
