package atf.autoex.ui;

/** Catalog activation and readiness without exposing the browser or API journal. */
public interface AutoExCatalogPagePort extends AutoExUiActions {
    void submitSearch(String query, String expectedProduct);
}
