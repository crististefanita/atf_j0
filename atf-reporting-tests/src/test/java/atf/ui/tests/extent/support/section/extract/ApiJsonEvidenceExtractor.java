package atf.ui.tests.extent.support.section.extract;

import atf.ui.dom.HtmlElement;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.reporting.audit.ReportAuditSnapshot;
import atf.reporting.api.evidence.ReportBodyAvailability;
import com.microsoft.playwright.Locator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Browser adapter that extracts raw request/response JSON facts without validating them. */
public final class ApiJsonEvidenceExtractor {
    private final ReportUiLocators locators;

    public ApiJsonEvidenceExtractor(ReportUiLocators locators) {
        this.locators = Objects.requireNonNull(locators, "locators");
    }

    public ReportAuditSnapshot.JsonEvidence extract(Locator requestNode) {
        Objects.requireNonNull(requestNode, "requestNode");
        List<String> violations = new ArrayList<>();
        var requestBodies = locators.requestJsonBodyElements(requestNode);
        var responseBodies = locators.responseJsonBodyElements(requestNode);
        var requestAvailability = availability(requestNode, "request", requestBodies);
        var responseAvailability = availability(requestNode, "response", responseBodies);
        String requestBody = body(requestBodies, "request", requestAvailability, violations);
        String responseBody = body(responseBodies, "response", responseAvailability, violations);
        return new ReportAuditSnapshot.JsonEvidence(requestBody, responseBody, violations,
                requestAvailability, responseAvailability);
    }

    private static ReportBodyAvailability availability(Locator request, String side, Locator bodies) {
        String attribute = "data-atf-" + side + "-body-availability";
        // Select only this exchange's metadata; a child/sibling cannot explain missing evidence.
        String value = (String) request.evaluate("(request, attribute) => {"
                + "const owner = request.closest('[data-atf-node-role=request]') || request;"
                + "const nodes = [...request.querySelectorAll('[' + attribute + ']')]"
                + ".filter(node => (node.closest('[data-atf-node-role=request]') || request) === owner);"
                + "return nodes.length === 1 ? nodes[0].getAttribute(attribute) : null; }", attribute);
        if (value != null) return ReportBodyAvailability.valueOf(value);
        return bodies.count() == 1 ? ReportBodyAvailability.AVAILABLE : ReportBodyAvailability.NOT_CAPTURED;
    }

    private static String body(Locator bodies, String side, ReportBodyAvailability availability,
                               List<String> violations) {
        if (availability != ReportBodyAvailability.AVAILABLE) {
            if (bodies.count() != 0) violations.add(side + " " + availability + " contradicts rendered payload");
            if (availability == ReportBodyAvailability.NOT_CAPTURED) violations.add(side + " body was not captured");
            return "";
        }
        if (bodies.count() != 1) {
            violations.add(side + " JSON body count must be exactly 1 but was " + bodies.count());
            return "";
        }
        Locator body = bodies.first();
        String tagName = String.valueOf(body.evaluate("element => element.tagName"));
        String value = HtmlElement.TEXTAREA.selector().equalsIgnoreCase(tagName)
                ? body.inputValue().trim() : Objects.requireNonNullElse(body.textContent(), "").trim();
        if (value.isBlank()) violations.add(side + " JSON body must not be blank");
        return value;
    }
}
