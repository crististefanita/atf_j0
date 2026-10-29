package atf.reporting.gherkin;

import atf.reporting.contract.AtfGherkinKeyword;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import java.util.List;
import static atf.reporting.contract.AtfGherkinKeyword.GIVEN;
import static atf.reporting.contract.AtfGherkinKeyword.WHEN;
import static atf.reporting.contract.AtfGherkinKeyword.THEN;
import static atf.reporting.contract.AtfGherkinKeyword.AND;
import static atf.reporting.contract.AtfGherkinKeyword.BUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest
class ReportGherkinSequenceTest {
    @Test void repeatedAnchorsBecomeContinuationsWithoutChangingTheirGroup() {
        var sequence = new ReportGherkinSequence();
        assertEquals(List.of(GIVEN, AND, WHEN, AND, THEN, AND),
                List.of(GIVEN, GIVEN, WHEN, WHEN, THEN, THEN).stream().map(sequence::next).toList());
    }
    @Test void explicitContinuationsAndNullInputDoNotReplaceTheLastAnchor() {
        var sequence = new ReportGherkinSequence();
        assertEquals(GIVEN, sequence.next(GIVEN));
        assertEquals(BUT, sequence.next(BUT));
        assertEquals(AND, sequence.next(AND));
        assertThrows(NullPointerException.class, () -> sequence.next(null));
        assertEquals(AND, sequence.next(GIVEN));
    }
    @Test void aFreshScenarioNeverInheritsAnotherScenariosNarrativeOrInventsAPrecondition() {
        var first = new ReportGherkinSequence();
        first.next(GIVEN); first.next(WHEN); first.next(THEN);
        var sibling = new ReportGherkinSequence();
        assertEquals(GIVEN, sibling.next(GIVEN));
        assertEquals(THEN, new ReportGherkinSequence().next(THEN));
    }
}
