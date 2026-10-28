package atf.shop.config.recovery;
import java.util.Set;
/** The catalog read is declared explicitly, independently of an HTTP verb and evidence channel. */
public enum ShopRecoveryOperation {
    CATALOG_READINESS("catalog-readiness");
    private final String key;
    ShopRecoveryOperation(String key) { this.key = key; }
    public String key() { return key; }
    public static Set<String> readOnlyNames() { return Set.of(CATALOG_READINESS.key); }
}
