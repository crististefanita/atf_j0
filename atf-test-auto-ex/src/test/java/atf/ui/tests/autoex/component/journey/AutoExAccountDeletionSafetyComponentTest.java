package atf.ui.tests.autoex.component.journey;

import atf.api.testkit.AtfComponentTest;
import atf.ui.autoex.support.account.AutoExAccountDeletionProgress;
import atf.ui.autoex.support.account.AutoExAccountDeletionAttempt.ObservedRequest;
import atf.ui.autoex.support.account.AutoExAccountDeletionAttempt.Traffic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static atf.api.core.http.request.HttpMethod.GET;
import static atf.api.core.http.request.HttpMethod.POST;

/** Pure business safety decisions: no browser, fake advert, injected hash or simulated shop. */
@AtfComponentTest
class AutoExAccountDeletionSafetyComponentTest {
    @ParameterizedTest
    @CsvSource({
            "true, 0, false, true",
            "true, 1, false, false",
            "true, 2, false, false",
            "true, 0, true, false",
            "true, 1, true, false",
            "false, 0, false, false",
            "false, 1, false, false",
            "false, 0, true, false"
    })
    void onlyAnAdvertCancelledBeforeAnyDeletionRequestAllowsNavigationToResume(
            boolean advertDismissed, int startedRequests, boolean destinationReached, boolean allowed) {
        var progress = new AutoExAccountDeletionProgress(advertDismissed, startedRequests, destinationReached);

        assertEquals(allowed, progress.mayResumeNavigation());
    }

    @Test
    void anInvalidRequestCountCannotAuthorizeAnotherDeletion() {
        assertThrows(IllegalArgumentException.class, () -> new AutoExAccountDeletionProgress(true, -1, false));
    }

    @Test
    void redirectDiagnosticsRetainOnlyThePathAndTransportFacts() {
        var observation = new ObservedRequest(GET.value(), "document", true,
                "https://demo:secret@store.example.test/delete_account?token=private#private");

        assertEquals("/delete_account", observation.redirectSourcePath());
        assertEquals("%s; resource=document; navigation=true; redirectedFrom=/delete_account"
                .formatted(GET.value()), observation.description());
    }

    @Test
    void anAbsentRedirectIsNotInventedForTheFirstRequest() {
        var observation = new ObservedRequest(GET.value(), "fetch", false, null);

        assertEquals("%s; resource=fetch; navigation=false; redirectedFrom=none"
                .formatted(GET.value()), observation.description());
    }

    @Test
    void supplementaryNonNavigationTrafficNeverAuthorizesReplayOrInventsASecondNavigation() {
        var requests = List.of(new ObservedRequest(GET.value(), "other", false, null),
                new ObservedRequest(GET.value(), "document", true, null));
        var traffic = new Traffic(requests.size(), requests);

        assertAll(
                () -> assertTrue(traffic.singleDeletionNavigation()),
                () -> assertEquals(1, traffic.navigationCount()),
                () -> assertEquals(2, traffic.observedRequests()),
                () -> assertTrue(traffic.description().contains("2 browser request event(s)")),
                () -> assertFalse(new AutoExAccountDeletionProgress(true, traffic.observedRequests(), false)
                        .mayResumeNavigation()));
    }

    @Test
    void duplicateNavigationsRemainAFailureEvenWhenTheirTransportFactsAreIdentical() {
        var navigation = new ObservedRequest(GET.value(), "document", true, null);
        var traffic = new Traffic(2, List.of(navigation, navigation));

        assertEquals(2, traffic.navigationCount());
        assertFalse(traffic.singleDeletionNavigation());
    }

    @Test
    void missingIncompleteOrUnexpectedNavigationCannotConfirmDeletion() {
        var navigation = new ObservedRequest(GET.value(), "document", true, null);
        assertAll(
                () -> assertFalse(new Traffic(0, List.of()).singleDeletionNavigation()),
                () -> assertFalse(new Traffic(1, List.of(new ObservedRequest(GET.value(), "other", false, null)))
                        .singleDeletionNavigation()),
                () -> assertFalse(new Traffic(2, List.of(navigation)).singleDeletionNavigation()),
                () -> assertFalse(new Traffic(1, List.of(new ObservedRequest(POST.value(), "document", true, null)))
                        .singleDeletionNavigation()),
                () -> assertFalse(new Traffic(1, List.of(new ObservedRequest(GET.value(), "other", true, null)))
                        .singleDeletionNavigation()));
    }

    @Test
    void aCompleteSingleNavigationSnapshotIsImmutableAndRejectsAnImpossibleCounter() {
        var requests = new ArrayList<>(List.of(new ObservedRequest(GET.value(), "document", true, null)));
        var traffic = new Traffic(1, requests);
        requests.clear();

        assertAll(
                () -> assertTrue(traffic.singleDeletionNavigation()),
                () -> assertThrows(UnsupportedOperationException.class, () -> traffic.requests().clear()),
                () -> assertThrows(IllegalArgumentException.class, () -> new Traffic(0, traffic.requests())));
    }
}
