package atf.console.capture;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class AtfConsoleOutputCaptureTest {
    @Test void observesAtEmissionTimeAndRestoresTheOriginalStream() throws Exception {
        var original = System.out;
        var lines = new ArrayList<String>();
        var result = AtfConsoleOutputCapture.capture(() -> {
            System.out.println("path with spaces Ș");
            assertEquals(1, lines.size());
            return 7;
        }, lines::add);
        assertEquals(7, result.result());
        assertEquals("path with spaces Ș" + System.lineSeparator(), result.output());
        assertSame(original, System.out);
    }

    @Test void actionFailurePreservesIdentityAndRestoresTheStream() {
        var original = System.out;
        var failure = new IllegalStateException("controlled action failure");
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> AtfConsoleOutputCapture.capture(() -> { throw failure; }, line -> { })));
        assertSame(original, System.out);
    }
}
