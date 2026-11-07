package atf.reporting.browser.process;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
@Tag("report-browser")
class OwnedChromiumProcessContractTest {
    @Test void theWorkerAndItsParentCannotBeClaimedForNativeDisposal() {
        ProcessHandle worker = ProcessHandle.current();
        ProcessHandle parent = worker.parent().orElseThrow();
        assertThrows(IllegalStateException.class, () -> OwnedChromiumProcess.ownedDescendant(worker.pid(), worker));
        assertThrows(IllegalStateException.class, () -> OwnedChromiumProcess.ownedDescendant(parent.pid(), worker));
        assertTrue(worker.isAlive() && parent.isAlive());
    }
}
