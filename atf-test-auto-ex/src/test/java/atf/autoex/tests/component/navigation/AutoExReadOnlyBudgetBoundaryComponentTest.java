package atf.autoex.tests.component.navigation;

import atf.autoex.runtime.navigation.AutoExStoreUnavailableException;
import atf.autoex.tests.extension.AutoExComponentTest;
import atf.autoex.tests.support.product.AutoExBorrowedBudgetProbe;
import org.junit.jupiter.api.Test;
import java.util.List;
import static atf.testkit.assertions.AuditedAssertions.*;

/** Complements deterministic UI arithmetic with the timeout values passed to a real HTTP/browser adapter. */
@AutoExComponentTest
class AutoExReadOnlyBudgetBoundaryComponentTest {
    @Test
    void successiveOwnedGetRecoveriesBorrowTheRemainingBudgetInsteadOfRenewingIt() throws Exception {
        var actual = AutoExBorrowedBudgetProbe.run();
        assertInstanceOf(AutoExStoreUnavailableException.class, actual.refusal().getCause());
        assertEquals(List.of("GET /product_details/1", "GET /product_details/1", "GET /product_details/1"), actual.requests(),
                "One initial document and exactly two eligible read-only recoveries are observed");
        assertEquals(1, actual.pointerActivations(), "One trusted product pointer causes the initial document; recovery never replays it");
        assertEquals(1, actual.timedClicks(), "One timed logical action owns the pointer and both native GET recoveries");
        assertEquals("FAIL", actual.finalActionStatus(), "Repeated HTTP 520 cannot publish a successful action");
        assertEquals(2, actual.nativeTimeouts().size(), "Both real native recovery calls expose their effective timeout");
        double first = actual.nativeTimeouts().getFirst(), second = actual.nativeTimeouts().getLast();
        assertTrue(first > 0 && first <= actual.allowanceBeforeRecovery(),
                "GET 1 timeout must be in (0, " + actual.allowanceBeforeRecovery() + "] ms; observed " + first + " ms");
        assertTrue(second > 0 && second < first,
                "GET 2 timeout must be in (0, " + first + ") ms; observed " + second + " ms");
    }
}
