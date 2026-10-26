package atf.ui.tests.extent.support.api;

import atf.reporting.contract.AtfReportStatus;
import com.microsoft.playwright.Page;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reusable browser flow for the declarative API-to-UI contract test.
 *
 * <p>Declarative JUnit classes provide intent and profile data only. All
 * Playwright types, navigation and request expansion stay in this flow.</p>
 */
public final class ApiReportUiContractFlow {
    public void execute(ApiReportUiFixture fixture, List<String> expectedOwners,
                        String auditTitle, Function<String, String> screenTitle) {
        Objects.requireNonNull(fixture, "fixture");
        expectedOwners = List.copyOf(Objects.requireNonNull(expectedOwners, "expectedOwners"));
        Objects.requireNonNull(auditTitle, "auditTitle");
        Objects.requireNonNull(screenTitle, "screenTitle");
        List<String> owners = expectedOwners;
        fixture.audit().audit(auditTitle, screen -> {
            Page page = fixture.flow().openTests();
            List<String> renderedOwners = fixture.flow().ownerNames(page);
            List<String> manifestOwners = fixture.setup().manifest().expectedOwnerNames();
            assertEquals(owners, manifestOwners,
                    "setup families must resolve to the same owner names as the mock manifest");
            assertEquals(manifestOwners, renderedOwners,
                    "Tests tab owners must exactly match the mock manifest selected by setup");
            for (int ownerIndex = 0; ownerIndex < owners.size(); ownerIndex++) {
                String ownerName = owners.get(ownerIndex);
                assertTrue(renderedOwners.contains(ownerName),
                        () -> ownerName + " must be rendered in the API report");
                if (ownerIndex > 0) fixture.flow().confirmTestsTabVisible(page, ownerName);
                ApiReportUiFlow.Owner owner = fixture.flow().openOwner(page, ownerName);
                int expandedRequests = fixture.flow().expandOwnerRequests(owner);
                var result = fixture.validation().inspect(owner);
                fixture.flow().recordOwnerContractAudit(owner, expandedRequests, result);
                var evidence = fixture.audit().capture(page,
                        ownerName + " request/response JSON contract");
                screen.screen(screenTitle.apply(ownerName), ownerName, evidence,
                        fixture.validation().assertions(result),
                        result.compliant() ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
                assertTrue(expandedRequests == result.requestCount(),
                        () -> ownerName + " expanded request count differs from inspected count");
                assertTrue(result.compliant(), () -> ownerName + ": " + result.violations());
            }
        });
    }
}
