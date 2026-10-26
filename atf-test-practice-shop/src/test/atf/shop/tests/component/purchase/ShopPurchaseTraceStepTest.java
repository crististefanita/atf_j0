package atf.shop.tests.component.purchase;

import atf.reporting.contract.AtfGherkinKeyword;
import atf.testkit.AtfComponentTest;
import atf.shop.support.ShopPurchaseTraceStep;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@AtfComponentTest
class ShopPurchaseTraceStepTest {
    @Test
    void guestPurchaseUsesOneContextOneActionSevenContinuationsAndOneOutcome() {
        var keywords = Arrays.stream(ShopPurchaseTraceStep.values())
                .filter(ShopPurchaseTraceStep::purchaseJourneyStep)
                .map(ShopPurchaseTraceStep::keyword)
                .toList();

        assertEquals(List.of(
                AtfGherkinKeyword.GIVEN,
                AtfGherkinKeyword.AND,
                AtfGherkinKeyword.AND,
                AtfGherkinKeyword.AND,
                AtfGherkinKeyword.WHEN,
                AtfGherkinKeyword.AND,
                AtfGherkinKeyword.AND,
                AtfGherkinKeyword.AND,
                AtfGherkinKeyword.AND,
                AtfGherkinKeyword.THEN), keywords);
    }

    @Test
    void allBusinessAndSetupPhrasesAreUniqueAndHaveNonBlankTitles() {
        var steps = Arrays.stream(ShopPurchaseTraceStep.values()).toList();
        var titles = steps.stream().map(ShopPurchaseTraceStep::title).toList();

        assertEquals(steps.size(), Set.copyOf(titles).size());
        org.junit.jupiter.api.Assertions.assertTrue(steps.stream()
                .allMatch(step -> step.keyword() != null && !step.title().isBlank()));
        org.junit.jupiter.api.Assertions.assertTrue(steps.stream()
                .anyMatch(step -> step == ShopPurchaseTraceStep.ACCOUNT_SETUP_FAILED));
    }
}
