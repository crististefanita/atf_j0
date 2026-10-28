package atf.ui.config.recovery;

import java.time.Duration;
import java.util.*;

/** Strict generic value decoder; consumers retain YAML I/O and their semantic operation registries. */
public final class UiRecoveryConfigurationParser {
    private static final Set<String> KEYS = Set.of("enabled", "profile", "operations", "maxAttempts", "totalTimeout", "backoff", "captureBackendSignals");
    private UiRecoveryConfigurationParser() { }
    public static UiRecoveryConfiguration parse(Map<?, ?> values, Set<String> knownReadOnlyOperations) {
        Objects.requireNonNull(values, "values");
        for (Object key : values.keySet()) if (!(key instanceof String) || !KEYS.contains(key)) throw new IllegalArgumentException("Unknown recovery key: " + key);
        var defaults = UiRecoveryConfiguration.disabled();
        var policy = new UiRecoveryConfiguration(bool(values, "enabled", false), text(values, "profile", defaults.profile()), operations(values),
                integer(values, "maxAttempts", defaults.maxAttempts()), duration(values, "totalTimeout", defaults.totalTimeout()),
                duration(values, "backoff", defaults.backoff()), bool(values, "captureBackendSignals", false), "YAML");
        policy.requireKnownReadOnlyOperations(knownReadOnlyOperations);
        return policy;
    }
    private static boolean bool(Map<?, ?> values, String key, boolean fallback) {
        if (!values.containsKey(key)) return fallback;
        if (!(values.get(key) instanceof Boolean value)) throw new IllegalArgumentException("recovery." + key + " must be a boolean");
        return value;
    }
    private static int integer(Map<?, ?> values, String key, int fallback) {
        if (!values.containsKey(key)) return fallback;
        Object value = values.get(key);
        if (!(value instanceof Integer || value instanceof Long)) throw new IllegalArgumentException("recovery." + key + " must be an integer");
        return Math.toIntExact(((Number) value).longValue());
    }
    private static String text(Map<?, ?> values, String key, String fallback) {
        if (!values.containsKey(key)) return fallback;
        if (!(values.get(key) instanceof String value) || value.isBlank()) throw new IllegalArgumentException("recovery." + key + " must be text");
        return value;
    }
    private static Duration duration(Map<?, ?> values, String key, Duration fallback) {
        return values.containsKey(key) ? Duration.parse(text(values, key, "")) : fallback;
    }
    private static Set<String> operations(Map<?, ?> values) {
        if (!values.containsKey("operations")) return Set.of();
        if (!(values.get("operations") instanceof List<?> list)) throw new IllegalArgumentException("recovery.operations must be an array");
        var result = new HashSet<String>();
        for (Object item : list) {
            if (!(item instanceof String name) || !name.matches("[a-z][a-z0-9-]{0,79}") || !result.add(name)) {
                throw new IllegalArgumentException("Recovery operations must be unique semantic names");
            }
        }
        return Set.copyOf(result);
    }
}
