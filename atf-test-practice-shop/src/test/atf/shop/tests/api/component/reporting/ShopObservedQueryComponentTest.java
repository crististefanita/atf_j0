package atf.shop.tests.api.component.reporting;

import atf.shop.support.api.ShopApiEvidenceMapper;
import atf.shop.support.api.ShopApiNetworkJournal;
import atf.shop.tests.support.ShopComponentTest;
import atf.reporting.ui.network.ReportNetworkInspector;
import org.junit.jupiter.api.Test;
import java.util.*;
import static atf.testkit.assertions.AuditedAssertions.*;

@ShopComponentTest
class ShopObservedQueryComponentTest {
    @Test void mapperKeepsEarlierDuplicateValuesInTheReportingIdentity() {
        var first=ShopApiEvidenceMapper.toExecution(exchange("tag=red&tag=last"));
        var second=ShopApiEvidenceMapper.toExecution(exchange("tag=blue&tag=last"));
        assertEquals(2,ReportNetworkInspector.aggregate(List.of(first,second),List.of()).signatures().size());
        assertEquals(2,first.request().queryParameters().values().size());
        assertEquals("last",first.request().query().get("tag"));
    }
    private static ShopApiNetworkJournal.Exchange exchange(String query) {
        return new ShopApiNetworkJournal.Exchange("GET","/products","https://fixture.invalid/products?"+query,
                200,"","{}",Map.of(),Map.of());
    }
    @Test void realJournalCarriesOpaqueIdentityAcrossTheConsumerCheckpointBoundary() {
        var journal=new ShopApiNetworkJournal(atf.shop.config.ShopValidationProfile.UI_API,
                    atf.shop.config.api.ShopApiEvidenceBinding.fromCsv(atf.shop.config.ShopValidationProfile.UI_API,"registration"));
        try(var inspector=new atf.ui.network.UiNetworkInspector(atf.ui.config.UiNetworkInspectionConfiguration.forHosts(List.of("fixture.invalid")))) {
            journal.attach(inspector);
            inspector.capture(observed("tag=red&tag=last",1));
            journal.registrationConfirmed(0,atf.core.http.protocol.ApiUris.httpsEndpoint("fixture.invalid"));
            var first=journal.evidence(atf.shop.config.api.ShopApiCheckpoint.REGISTRATION).getFirst();
            long marker=journal.sequenceMarker();
            inspector.capture(observed("tag=blue&tag=last",2));
            journal.registrationConfirmed(marker,atf.core.http.protocol.ApiUris.httpsEndpoint("fixture.invalid"));
            var second=journal.evidence(atf.shop.config.api.ShopApiCheckpoint.REGISTRATION).getFirst();
            assertEquals(2,ReportNetworkInspector.aggregate(List.of(first,second),List.of()).signatures().size());
            assertTrue(first.request().queryParameters().isEmpty());
            assertEquals(64,first.request().queryFingerprint().length());
        }
    }
    private static atf.ui.network.UiNetworkObservation observed(String query,long sequence) {
        return new atf.ui.network.UiNetworkObservation(sequence,java.time.Instant.EPOCH,"POST",
                "https://fixture.invalid/users/register?"+query,"/users/register",201,Map.of(),Map.of(),"{}","{}",
                "none","none","fixture","UI_BROWSER","",true,"");
    }
}
