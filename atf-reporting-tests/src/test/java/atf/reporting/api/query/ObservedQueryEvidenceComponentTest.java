package atf.reporting.api.query;

import atf.reporting.api.*;

import atf.reporting.contract.*;
import atf.reporting.api.evidence.ApiExchangeEvidence;
import atf.reporting.api.evidence.ReportBodyAvailability;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.ui.network.ReportNetworkInspector;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class ObservedQueryEvidenceComponentTest {
    @TempDir Path directory;
    @ParameterizedTest @ValueSource(booleans={true,false})
    void completeQueryPairsSurviveTheAggregateAndBothArtifacts(boolean extent) throws Exception {
        var first=evidence("tag=red&tag=last&token=query-private-canary",1);
        var second=evidence("tag=blue&tag=last&token=query-private-canary",2);
        var aggregate=ReportNetworkInspector.aggregate(List.of(first,second),List.of());
        assertEquals(2,aggregate.signatures().size());
        assertEquals(3,first.request().toBuilder().build().queryParameters().values().size());
        assertEquals("last",first.request().query().get("tag"));
        Path output=directory.resolve(extent?"extent.html":"fallback.html");
        try(AtfReportPorts.Session session=extent?new ExtentReportSession(output):new HtmlReportSession(output);
            var scenario=session.startScenario("Query","ordered observations",List.of("component"));
            var phase=scenario.startPhase("TEST BODY")) {
            for(var exchange:List.of(first,second)) {
                try(var node=phase.startRequest("Observed query","GET","/products")) {
                    ApiExchangeEvidence.publish(node,exchange);node.complete(AtfReportStatus.PASS);
                }
            }
            phase.complete(AtfReportStatus.PASS);scenario.complete(AtfReportStatus.PASS);
        }
        String html=Files.readString(output);
        assertTrue(html.contains("tag=red"));assertTrue(html.contains("tag=blue"));
        assertFalse(html.contains("query-private-canary"));
    }
    @Test void encodedValuesEmptyValuesAndDuplicateOrderRemainDistinctWithoutChangingLegacyMapCallers() {
        var first=evidence("tag=red&tag=last&empty=&encoded=a%26b",1).request();
        var reordered=evidence("tag=last&tag=red&empty=&encoded=a%26b",2).request();
        assertEquals(List.of("red","last","","a&b"),first.queryParameters().values().stream().map(ReportQueryParameter::value).toList());
        assertNotEquals(ReportRequestSignature.forRequest(first),ReportRequestSignature.forRequest(reordered));
        assertEquals(Map.of("page","1"),new ReportRequest("GET","/products",Map.of("page","1"),Map.of(),"none","").query());
    }
    private static ReportExecutionEvidence evidence(String query,long sequence) {
        var uri=URI.create("https://fixture.invalid/products?"+query);
        var request=ReportRequest.builder("GET","/products").queryParameters(ReportQueryParameters.fromUri(uri))
                .body("",ReportBodyAvailability.NOT_CAPTURED).build();
        var response=new ReportResponse(200,"{}",Map.of(),null,uri.toString());
        var trace=ReportTraceMetadata.from(request).withCapture("",sequence,"TEST BODY","api","","fetch","UI_BROWSER","","","");
        return new ReportExecutionEvidence(response,1,null,List.of(),List.of(),request,ReportTransportProvenance.LIVE_HTTP,trace);
    }
    @ParameterizedTest @ValueSource(booleans={true,false})
    void opaqueObservedQueriesRemainDistinctWithoutPublishingTheirFingerprints(boolean extent) throws Exception {
        String one="a24f57013f119d690c6380787976dbd1036de98f8257464a493155c7ac9d9f14";
        String two="b7bb1ba0c628f105ad349b96f06c955bdf4ff5bf71fab23f0260b3f7ef5fc7d1";
        var executions=List.of(opaque(one),opaque(two));
        assertEquals(2,ReportNetworkInspector.aggregate(executions,List.of()).signatures().size());
        assertEquals(one,executions.getFirst().request().toBuilder().build().queryFingerprint());
        Path output=directory.resolve(extent?"opaque-extent.html":"opaque-fallback.html");
        try(AtfReportPorts.Session session=extent?new ExtentReportSession(output):new HtmlReportSession(output);
            var scenario=session.startScenario("Query","opaque captured queries",List.of("component"));
            var phase=scenario.startPhase("TEST BODY")) {
            for(var execution:executions) try(var node=phase.startRequest("Observed query","GET","/products")) {
                ApiExchangeEvidence.publish(node,execution);node.networkAggregate(ReportNetworkInspector.aggregate(executions,List.of()));
                node.complete(AtfReportStatus.PASS);
            }
            phase.complete(AtfReportStatus.PASS);scenario.complete(AtfReportStatus.PASS);
        }
        String html=Files.readString(output);assertFalse(html.contains(one));assertFalse(html.contains(two));
        assertTrue(html.contains("Not captured"));
    }
    private static ReportExecutionEvidence opaque(String fingerprint) {
        var request=ReportRequest.builder("GET","/products").queryFingerprint(fingerprint).build();
        return new ReportExecutionEvidence(new ReportResponse(200,"{}",Map.of(),null,"https://fixture.invalid/products?redacted-query"),
                1,null,List.of(),List.of(),request,ReportTransportProvenance.LIVE_HTTP);
    }
    @Test void changingKnownQueryCannotReuseThePreviousCaptureFingerprint() {
        var request=opaque("a24f57013f119d690c6380787976dbd1036de98f8257464a493155c7ac9d9f14").request();
        assertEquals("",request.toBuilder().query(Map.of("page","2")).build().queryFingerprint());
        assertThrows(IllegalArgumentException.class,()->ReportRequest.builder("GET","/products").queryFingerprint("plaintext-canary"));
    }
}
