package atf.shop.tests.support.lifecycle;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
                        () -> { throw failure; }, published::set, () -> { }));

        assertSame(failure, thrown);
        assertSame(failure, published.get());
    }

    @Test
    void keepsBusinessFailurePrimaryWhenPublicationAndCloseAlsoFail() {
        var businessFailure = new IllegalStateException("business assertion failed");
        var publicationFailure = new IllegalArgumentException("report publication failed");
        var closeFailure = new IllegalStateException("scenario close failed");

        var thrown = assertThrows(IllegalStateException.class,
                () -> ShopE2eContext.Scenario.executeWithFailureCapture(
                        () -> { throw businessFailure; },
                        failure -> { throw publicationFailure; },
                        () -> { throw closeFailure; }));

        assertSame(businessFailure, thrown);
        assertEquals(List.of(publicationFailure, closeFailure),
                Arrays.asList(thrown.getSuppressed()));
    }

    @Test
    void closeFailureRemainsVisibleWhenBusinessBodyPasses() {
        var closeFailure = new IllegalStateException("scenario close failed");

        var thrown = assertThrows(IllegalStateException.class,
                () -> ShopE2eContext.Scenario.executeWithFailureCapture(
                        () -> { }, failure -> { throw new AssertionError("unexpected publication"); },
                        () -> { throw closeFailure; }));

        assertSame(closeFailure, thrown);
    }
}
