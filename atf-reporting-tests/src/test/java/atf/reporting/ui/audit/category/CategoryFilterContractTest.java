package atf.reporting.ui.audit.category;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class CategoryFilterContractTest {
    @Test
    void acceptsZeroOneAndManyWellFormedScenarioLinks() {
        assertTrue(CategoryFilterContract.validateLinkedScenarioIds(0, List.of()).valid());
        assertTrue(CategoryFilterContract.validateLinkedScenarioIds(1, List.of("12")).valid());
        assertTrue(CategoryFilterContract.validateLinkedScenarioIds(
                3, List.of("12", "13", "14")).valid());
    }

    @Test
    void rejectsWrongCardinalityAndMalformedScenarioIds() {
        assertFalse(CategoryFilterContract.validateLinkedScenarioIds(1, List.of()).valid());
        assertFalse(CategoryFilterContract.validateLinkedScenarioIds(
                2, List.of("12", "bad")).valid());
        assertFalse(CategoryFilterContract.validateLinkedScenarioIds(-1, List.of()).valid());
    }
}
