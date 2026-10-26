package atf.shop.tests.api.support;

import java.util.List;

/** Stable report metadata for the standalone Practice Shop API evidence test. */
public final class ShopApiReportMetadataFixture {
    public static final String LIVE_TEST_CLASS = "ShopStandaloneApiReportTest";
    public static final String LIVE_TEST_PACKAGE = "atf.shop.tests.api.integration";
    public static final String LIVE_SCENARIO = "Practice Shop live API business flow";
    public static final List<String> LIVE_TAGS = List.of("api", "integration", "live", "practice-shop");

    public static final String COMPONENT_TEST_CLASS = "ShopApiContractReportComponentTest";
    public static final String COMPONENT_TEST_PACKAGE = "atf.shop.tests.api.component.reporting";
    public static final String COMPONENT_SCENARIO = "Practice Shop API component contract";
    public static final List<String> COMPONENT_TAGS = List.of("api", "component", "acceptance", "practice-shop");

    /** @deprecated use the explicit live/component metadata above. */
    @Deprecated public static final String TEST_CLASS = LIVE_TEST_CLASS;
    /** @deprecated use the explicit live/component metadata above. */
    @Deprecated public static final String TEST_PACKAGE = LIVE_TEST_PACKAGE;
    /** @deprecated use the explicit live/component metadata above. */
    @Deprecated public static final String SCENARIO = LIVE_SCENARIO;
    /** @deprecated use the explicit live/component metadata above. */
    @Deprecated public static final List<String> TAGS = LIVE_TAGS;

    private ShopApiReportMetadataFixture() { }
}
