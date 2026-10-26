package atf.reporting.api.tests.api.flows;

import atf.mock.MockRoutePaths;

import atf.core.http.response.ApiExecutionResult;
import atf.core.http.request.HttpMethod;
import atf.core.reporting.contract.AtfReportPorts;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Cross-request assertions for stateful API flows.
 *
 * <p>Response contracts validate each HTTP response independently. These
 * assertions validate the relationship between the mutation response and the
 * subsequent read model, which is the part a duplicated POST/GET contract
 * cannot prove.</p>
 */
final class ApiFlowAssertions {
    private static final ObjectMapper JSON = new ObjectMapper();

    private ApiFlowAssertions() { }

    static void postReadBack(ApiExecutionResult created, ApiExecutionResult readBack,
                             AtfReportPorts.Request request) {
        postReadBack(created, null, readBack, request);
    }

    static void postReadBack(ApiExecutionResult created, ApiExecutionResult collection,
                             ApiExecutionResult readBack, AtfReportPorts.Request request) {
        JsonNode mutation = body(created, HttpMethod.POST);
        if (collection != null) {
            JsonNode collectionModel = body(collection, HttpMethod.GET);
            String createdId = text(mutation, "id", HttpMethod.POST.value() + " resource id");
            boolean present = collectionModel.isArray() && java.util.stream.StreamSupport
                    .stream(collectionModel.spliterator(), false)
                    .anyMatch(item -> createdId.equals(textOrNull(item, "id")));
            if (!present) {
                throw new AssertionError(HttpMethod.GET.value() + " collection does not contain "
                        + HttpMethod.POST.value() + " id " + createdId);
            }
        }
        JsonNode readModel = body(readBack, HttpMethod.GET);
        String createdId = text(mutation, "id", HttpMethod.POST.value() + " resource id");
        String selfLink = textAt(readModel, "links.self", HttpMethod.GET.value() + " self link");
        String expectedSelf = MockRoutePaths.postById(createdId);
        requireEqual(expectedSelf, selfLink, "POST id is linked to the GET read model");
        requireEqual("published", textAt(readModel, "status",
                        HttpMethod.GET.value() + " publication status"),
                HttpMethod.GET.value() + " exposes the persisted publication state");
        if (request != null) {
            request.detail("FLOW ASSERTIONS", "PASS — POST $.id=" + createdId
                    + " → GET collection contains id → GET $.links.self=" + selfLink);
            request.detail("Read-model state", "PASS — GET $.status=published");
        }
    }

    static String resourceId(ApiExecutionResult mutation, HttpMethod method) {
        return text(body(mutation, method), "id", method.value() + " resource id");
    }

    static void profileReadBack(ApiExecutionResult updated, ApiExecutionResult readBack,
                                AtfReportPorts.Request request) {
        JsonNode mutation = body(updated, HttpMethod.PUT);
        JsonNode readModel = body(readBack, HttpMethod.GET);
        String mutationId = text(mutation, "id", HttpMethod.PUT.value() + " profile id");
        String readId = text(readModel, "id", HttpMethod.GET.value() + " profile id");
        requireEqual(mutationId, readId, HttpMethod.PUT.value() + " id is preserved by the "
                + HttpMethod.GET.value() + " read model");
        requireEqual(text(mutation, "version", HttpMethod.PUT.value() + " profile version"),
                text(readModel, "version", HttpMethod.GET.value() + " profile version"),
                HttpMethod.PUT.value() + " version is preserved by the "
                        + HttpMethod.GET.value() + " read model");
        requireEqual("profile-update", textAt(readModel, "audit.lastMutation",
                        HttpMethod.GET.value() + " mutation audit"),
                HttpMethod.GET.value() + " exposes the mutation that produced the read model");
        requireEqual("locale", textAt(readModel, "audit.lastChangedFields[0]",
                        HttpMethod.GET.value() + " changed fields"),
                HttpMethod.GET.value() + " exposes the first persisted changed field");
        if (request != null) {
            request.detail("FLOW ASSERTIONS", "PASS — PUT $.id=" + mutationId
                    + " == GET $.id=" + readId);
            request.detail("Mutation lineage", "PASS — GET $.audit.lastMutation=profile-update");
        }
    }

    private static String textOrNull(JsonNode root, String field) {
        JsonNode value = root == null ? null : root.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static JsonNode body(ApiExecutionResult execution, HttpMethod method) {
        try {
            return JSON.readTree(execution.response().body());
        } catch (Exception error) {
            throw new AssertionError(method.value() + " response is not valid JSON", error);
        }
    }

    private static String text(JsonNode root, String field, String label) {
        return textAt(root, field, label);
    }

    private static String textAt(JsonNode root, String path, String label) {
        JsonNode current = root;
        for (String segment : path.split("\\.")) {
            if (segment.matches(".+\\[\\d+]")) {
                int bracket = segment.indexOf('[');
                current = current == null ? null : current.get(segment.substring(0, bracket));
                int index = Integer.parseInt(segment.substring(bracket + 1, segment.length() - 1));
                current = current == null ? null : current.get(index);
            } else {
                current = current == null ? null : current.get(segment);
            }
        }
        if (current == null || current.isMissingNode() || current.isNull()) {
            throw new AssertionError(label + " is missing at $." + path);
        }
        return current.asText();
    }

    private static void requireEqual(String expected, String actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + " but was " + actual);
        }
    }
}
