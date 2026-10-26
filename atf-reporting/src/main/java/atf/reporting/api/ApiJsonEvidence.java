package atf.reporting.api;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.api.evidence.ApiExchangeEvidence;

import java.util.List;

/**
 * Legacy JSON-only adapter for callers without typed HTTP capture evidence.
 *
 * <p>Its historical empty-to-JSON-null convention is not evidence of a captured
 * HTTP body. New publishers use {@link ApiExchangeEvidence}
 * and its explicit availability model instead.</p>
 */
public final class ApiJsonEvidence {
    public static final String REQUEST_LABEL = "REQUEST JSON";
    public static final String RESPONSE_LABEL = "RESPONSE JSON";
    public static final String EMPTY_JSON = ReportJsonBodyPolicy.NULL_DOCUMENT;

    private ApiJsonEvidence() { }

    public static void publish(AtfReportPorts.Request request,
                               String requestBody,
                               String responseBody) {
        request.json(REQUEST_LABEL, ReportJsonBodyPolicy.display(requestBody));
        request.json(RESPONSE_LABEL, ReportJsonBodyPolicy.display(responseBody));
    }

    /** Test/consumer migration overload for the former core request port. */
    public static void publish(Object request, String requestBody, String responseBody) {
        try {
            var json = request.getClass().getMethod("json", String.class, String.class);
            json.trySetAccessible();
            json.invoke(request, REQUEST_LABEL, ReportJsonBodyPolicy.display(requestBody));
            json.invoke(request, RESPONSE_LABEL, ReportJsonBodyPolicy.display(responseBody));
        } catch (ReflectiveOperationException error) {
            throw new IllegalArgumentException("request does not expose the reporting json port", error);
        }
    }

    public static String display(String body) {
        return ReportJsonBodyPolicy.display(body);
    }

    public static boolean hasBody(String body) {
        return body != null && !body.isBlank();
    }

    /**
     * Returns the write-side exchange violations that must be visible in the
     * report contract. This legacy JSON contract applies only to POST/PUT;
     * it does not infer capture availability for the other HTTP methods.
     */
    public static List<String> writeContractViolations(String method,
                                                        String requestBody,
                                                        String responseBody) {
        return ReportJsonBodyPolicy.appliesTo(method)
                ? ReportJsonBodyPolicy.validateWriteExchange(requestBody, responseBody)
                : List.of();
    }
}
