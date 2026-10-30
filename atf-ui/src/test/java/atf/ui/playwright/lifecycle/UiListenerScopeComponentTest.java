package atf.ui.playwright.lifecycle;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class UiListenerScopeComponentTest {
    @Test void scopeBecomesActiveOnlyAfterEverySubscriptionAndClosesBeforeAnyWithdrawal() {
        var scope = new UiListenerScope(); var removed = new ArrayList<Integer>();
        for (int index=0; index<4; index++) {
            int item=index;
            scope.add(() -> assertFalse(scope.active()), () -> {assertFalse(scope.active());removed.add(item);});
        }
        assertFalse(scope.active()); scope.attach(); assertTrue(scope.active()); scope.close(); scope.close();
        assertEquals(List.of(0,1,2,3), removed);
        assertThrows(IllegalStateException.class, scope::attach);
        assertThrows(IllegalStateException.class, () -> scope.add(() -> {}, () -> {}));
    }
    @Test void rollbackCombinesRepeatedAndDistinctFailuresWithoutCycles() {
        var scope = new UiListenerScope(); var primary = new AssertionError("acquire");
        var secondary = new IllegalStateException("withdraw"); var removed = new ArrayList<Integer>();
        scope.add(() -> {}, () -> {removed.add(1);throw primary;})
                .add(() -> {throw primary;}, () -> {removed.add(2);throw secondary;})
                .add(() -> fail("later attach must not run"), () -> {removed.add(3);throw secondary;});
        assertSame(primary, assertThrows(AssertionError.class, scope::attach));
        assertArrayEquals(new Throwable[]{secondary}, primary.getSuppressed());
        assertEquals(List.of(1,2,3), removed); assertFalse(scope.active()); scope.close();
    }
}
