package atf.core.config.module;

import java.util.Locale;
import java.util.Objects;

/** A logical ATF module that can participate in a configured test plan. */
public enum AtfModule {
    API("api"),
    REPORTING("reporting"),
    UI("ui"),
    MOCK("mock"),
    CONSOLE("console"),
    DEBUG_UI("debug-ui"),
    CORE("core"),
    VALIDATION("validation");

    private final String key;

    AtfModule(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static AtfModule fromKey(String value) {
        String normalized = Objects.requireNonNull(value, "value")
                .trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (AtfModule module : values()) {
            if (module.key.equals(normalized)) return module;
        }
        throw new IllegalArgumentException("Unknown ATF module: " + value);
    }
}
