package atf.mock.json;

import atf.core.http.request.ApiRequest;
import atf.mock.*;
import atf.testkit.AtfComponentTest;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class MockNumericContractComponentTest {
    @TempDir java.nio.file.Path temporary;
    static Stream<Arguments> distinct() {return Stream.of(
            Arguments.of("9007199254740992.0","9007199254740993.0"),
            Arguments.of("-9007199254740992.0","-9007199254740993.0"),
            Arguments.of("0.10000000000000000000000000001","0.10000000000000000000000000002"),
            Arguments.of("1e309","2e309"),Arguments.of("1e-400","2e-400"));}
    static Stream<Arguments> nominal() {return Stream.of(
            Arguments.of("9007199254740993.0","9007199254740993.0"),
            Arguments.of("1.2300","1.23"),Arguments.of("1e2","100.0"),
            Arguments.of("0","0"),Arguments.of("-0.0","-0.0"),Arguments.of("1e309","10e308"));}
    @ParameterizedTest @MethodSource("distinct")
    void distinctNumbersSelectFallback(String expected,String actual) {
        var route=route(expected);
        var request=request(actual);
        assertFalse(route.matches(request));
        var transport=new AtfMockTransport(new AtfMockConfiguration(List.of(route),
                new MockRoute("*","/*",499,"fallback",Map.of(),Duration.ZERO)));
        assertEquals(499,transport.send(request,"/numeric","none").join().status());
        assertEquals(body(actual),request.bodyText(),"matching does not rewrite the wire body");
    }
    @ParameterizedTest @MethodSource("nominal")
    void completeEqualNumbersRemainEligible(String expected,String actual) {
        assertTrue(route(expected).matches(request(actual)));
    }
    @Test void keepsStringNumberAndIntegerDecimalNodeCompatibility() {
        assertFalse(route("1").matches(request("\"1\"")));
        assertFalse(route("1").matches(request("1.0")),"retain the existing integer/decimal node distinction");
    }
    @Test void preservesAuthQueryMimeAndConfiguredResponseText() {
        String response="{\"accepted\":1.2300}";
        var route=new MockRoute("POST","/numeric",201,response,Map.of(),Duration.ZERO,"*",Map.of(),Map.of(),"json",body("1.23"));
        assertEquals(response,route.response("/numeric").body());
        assertFalse(route.matches(new ApiRequest(HttpRequest.BodyPublishers.ofString(body("1.23")),"POST","/numeric",
                Map.of(),Map.of("Content-Type","text/plain"),"none",null,body("1.23"))));
    }
    @Test void yamlNumericConstraintKeepsItsOriginalValueBeforeMatching() throws Exception {
        var yaml=temporary.resolve("numeric.yaml");
        java.nio.file.Files.writeString(yaml,"mock:\n  routes:\n    - method: POST\n      path: /numeric\n      status: 201\n"
                +"      body: '{}'\n      request-body: 9007199254740993.0\n");
        var route=AtfMockConfigurationLoader.load(yaml).routes().getFirst();
        assertEquals(0,new java.math.BigDecimal("9007199254740993.0").compareTo(
                new java.math.BigDecimal(route.expectedRequestBody())),"YAML does not narrow the constraint");
        var correct=new ApiRequest(HttpRequest.BodyPublishers.ofString("9007199254740993.0"),"POST","/numeric",Map.of(),
                Map.of("Content-Type","application/json"),"none",null,"9007199254740993.0");
        var distinct=new ApiRequest(HttpRequest.BodyPublishers.ofString("9007199254740992.0"),"POST","/numeric",Map.of(),
                Map.of("Content-Type","application/json"),"none",null,"9007199254740992.0");
        assertTrue(route.matches(correct));assertFalse(route.matches(distinct));
    }
    private static MockRoute route(String expected) {
        return new MockRoute("POST","/numeric",201,"{}",Map.of(),Duration.ZERO,"*",Map.of(),Map.of(),"json",body(expected));
    }
    private static ApiRequest request(String number) {
        String value=body(number);
        return new ApiRequest(HttpRequest.BodyPublishers.ofString(value),"POST","/numeric",Map.of(),
                Map.of("Content-Type","application/json"),"none",null,value);
    }
    private static String body(String number) {return "{\"amount\":"+number+"}";}
}
