package atf.autoex.config.recovery;
import java.util.Set;
/** Application-owned semantic allowlist; GET cart/account routes are deliberately absent. */
public enum AutoExRecoveryOperation {
    CATALOG_READINESS("catalog-readiness");
    private final String key;
    AutoExRecoveryOperation(String key) { this.key = key; }
    public String key() { return key; }
    public static Set<String> readOnlyNames() { return Set.of(CATALOG_READINESS.key); }
}
