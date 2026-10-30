package atf.ui.network;

import atf.ui.config.UiNetworkInspectionConfiguration;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class UiObservedQueryFingerprintComponentTest {
    @Test void queryIdentitySurvivesRealInspectorSanitizationWithoutRetainingAnyQueryValue() {
        var configuration=UiNetworkInspectionConfiguration.forHosts(List.of("fixture.invalid"));
        try(var inspector=new UiNetworkInspector(configuration)) {
            var first=inspector.capture(observed("page=1&token=private-query-canary",1)).orElseThrow();
            var second=inspector.capture(observed("page=2&token=private-query-canary",2)).orElseThrow();
            inspector.capture(observed("page=1&token=private-query-canary",3));
            assertEquals(first.url(),second.url());assertTrue(first.url().endsWith("?redacted-query"));
            assertFalse(first.toString().contains("private-query-canary"));assertFalse(first.url().contains("page="));
            assertEquals(64,first.queryFingerprint().length());assertNotEquals(first.queryFingerprint(),second.queryFingerprint());
            assertEquals(first.queryFingerprint(),first.sanitizedFor(configuration).queryFingerprint());
            assertEquals(2,inspector.signatureCounts().size());assertEquals(2,inspector.signatureCounts().get(first.requestSignature()));
        }
    }
    private static UiNetworkObservation observed(String query,long sequence) {
        return new UiNetworkObservation(sequence,Instant.EPOCH,"GET","https://fixture.invalid/products?"+query,
                "/products",200,Map.of(),Map.of(),"","{}","none","none","fixture","UI_BROWSER","",true,"");
    }
}
