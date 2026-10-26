package atf.shop.tests.support.lifecycle;

import atf.api.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest
class ShopE2eFailureBoundaryTest {
    @Test
    void publishesLinkageErrorsAndRethrowsTheOriginalFailure() {
        var failure = new NoClassDefFoundError("missing runtime class");
        var published = new AtomicReference<Throwable>();

        var thrown = assertThrows(NoClassDefFoundError.class,
                () -> ShopE2eContext.Scenario.executeWithFailureCapture(
                        () -> { throw failure; }, published::set));

        assertSame(failure, thrown);
        assertSame(failure, published.get());
    }
}
