package atf.core.http.request.json;

import atf.core.http.request.ApiRoute;
import atf.testkit.AtfUnitTest;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class JsonWriteBoundaryTest {
    @ParameterizedTest
    @ValueSource(strings={"{} trailing", "{} {}", "{\"x\":1,\"x\":2}", "null"})
    void rejectsInvalidJsonBeforeSubmission(String body) {
        for(String method:java.util.List.of("POST","PUT")) {
            var submitted=new AtomicInteger();
            var route=new ApiRoute("write",method,"/orders","orders","create","none");
            assertThrows(IllegalArgumentException.class,()->{
                route.requestBody(body,Map.of());
                submitted.incrementAndGet();
            });
            assertEquals(0,submitted.get(),"JSON factory refuses the request before its caller can submit it");
        }
    }
}
