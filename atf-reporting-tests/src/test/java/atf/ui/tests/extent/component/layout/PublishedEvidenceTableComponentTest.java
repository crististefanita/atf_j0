package atf.ui.tests.extent.component.layout;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.table.PublishedEvidenceTableFixture.Profile;
import atf.ui.tests.extent.support.inspection.table.PublishedEvidenceTableProbe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.stream.Stream;

/** Real publications, with an independent visual oracle rather than allocator-derived percentages. */
@AtfComponentTest
class PublishedEvidenceTableComponentTest {
    @ParameterizedTest(name = "{0}; fallback={1}") @MethodSource("profiles")
    void allPublishedTablesRemainReadableAcrossProfilesWidthsAndDisclosures(Profile profile, boolean fallback) {
        PublishedEvidenceTableProbe.verify(profile, fallback);
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void visualOracleRejectsClippingMissingColumnsGapsAndChangedAssertionEvidence(boolean fallback) {
        PublishedEvidenceTableProbe.verifyNegative(fallback);
    }

    static Stream<Arguments> profiles() {
        return Arrays.stream(Profile.values()).flatMap(profile ->
                Stream.of(Arguments.of(profile, false), Arguments.of(profile, true)));
    }

    @Test
    void visualOracleRejectsNavigationOcclusionAndTechnicalHeaderWrapping() {
        PublishedEvidenceTableProbe.verifyNativeVisualDefects();
    }
}

