package atf.core;
import atf.testkit.AtfUnitTest;


import atf.core.config.ApiTransportMode;
import atf.core.config.ApiTransportPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest

class ApiTransportPolicyTest {
    @Test
    void acceptanceIsTheSafeTypedDefault() {
        assertEquals(ApiTransportMode.ACCEPTANCE, ApiTransportMode.parse(null));
        assertEquals(ApiTransportMode.ACCEPTANCE, ApiTransportPolicy.acceptance().mode());
    }

    @Test
    void liveRequiresAnExplicitOptIn() {
        assertThrows(IllegalArgumentException.class,
                () -> new ApiTransportPolicy(ApiTransportMode.LIVE, false));
        assertEquals(ApiTransportMode.LIVE, ApiTransportPolicy.explicitLive().mode());
    }
}
