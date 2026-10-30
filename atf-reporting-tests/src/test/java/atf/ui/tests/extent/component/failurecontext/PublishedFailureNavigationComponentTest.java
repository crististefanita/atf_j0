package atf.ui.tests.extent.component.failurecontext;

import atf.reporting.support.failurecorrelation.FailureNavigationFixture.Owner;
import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.failurenavigation.FailureNavigationProbe;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@AtfComponentTest
class PublishedFailureNavigationComponentTest {
    @ParameterizedTest @CsvSource({"UI,392", "UI,1280", "API,392", "API,1280", "COMPONENT,392", "COMPONENT,1280"})
    void failureBacklinksRevealTheExactComparisonAcrossPhasesWithoutHarnessExpansion(Owner owner, int width) {
        FailureNavigationProbe.verify(owner, width);
    }
}
