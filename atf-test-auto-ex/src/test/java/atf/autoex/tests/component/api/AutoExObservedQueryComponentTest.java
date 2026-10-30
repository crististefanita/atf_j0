package atf.autoex.tests.component.api;

import atf.autoex.runtime.AutoExObservedExchangeMapper;
import atf.autoex.tests.extension.AutoExComponentTest;
import atf.reporting.ui.network.ReportNetworkInspector;
import atf.ui.network.UiNetworkObservation;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static atf.testkit.assertions.AuditedAssertions.*;

@AutoExComponentTest
class AutoExObservedQueryComponentTest {
    @Test void browserMapperRetainsDistinctQueriesAndTheConsumerService() {
        try(var inspector=new atf.ui.network.UiNetworkInspector(atf.ui.config.UiNetworkInspectionConfiguration.forHosts(List.of("fixture.invalid")))) {
            var one=inspector.capture(observed("page=1",1)).orElseThrow();
            var two=inspector.capture(observed("page=2",2)).orElseThrow();
            var first=AutoExObservedExchangeMapper.map(one);
            var second=AutoExObservedExchangeMapper.map(two);
            assertEquals(2,ReportNetworkInspector.aggregate(List.of(first,second),List.of()).signatures().size());
            assertTrue(first.request().queryParameters().isEmpty(),"Removed query values are not reconstructed");
            assertEquals(one.queryFingerprint(),first.request().queryFingerprint());
            assertEquals("fixture",first.trace().service());
        }
    }
    private static UiNetworkObservation observed(String query,long sequence) {
        return new UiNetworkObservation(sequence,Instant.EPOCH,"GET","https://fixture.invalid/products?"+query,
                "/products",200,Map.of(),Map.of(),"","{}","none","none","fixture","UI_BROWSER","",true,"");
    }
}
