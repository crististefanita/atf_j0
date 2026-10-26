package atf.shop.tests.api.support;

import atf.core.http.response.ApiExecutionResult;
import atf.reporting.contract.AtfAssertion;
import atf.shop.api.contract.ShopApiHttpStatus;
import atf.shop.support.api.ShopApiReportValidation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Test-thread scope that pairs each captured exchange with its explicit expected response. */
public final class ShopApiCallAuditContextTestSupport {
    private static final ThreadLocal<AuditContext> CONTEXT = new ThreadLocal<>();

    private ShopApiCallAuditContextTestSupport() { }

    public static void recordWith(Consumer<CapturedExchange> recorder) {
        CONTEXT.set(new AuditContext(Objects.requireNonNull(recorder, "recorder")));
    }

    /** Declares the next transport exchange's contract before that exchange executes. */
    public static void expectNext(String description, ShopApiHttpStatus expectedStatus) {
        current().expect(new ExpectedExchange(description, description, expectedStatus));
    }

    /** Declares report intent separately from the human-readable contract expectation. */
    public static void expectNext(String operationIntent, String description,
                                  ShopApiHttpStatus expectedStatus) {
        current().expect(new ExpectedExchange(operationIntent, description, expectedStatus));
    }

    public static void capture(ApiExecutionResult execution) {
        AuditContext context = CONTEXT.get();
        if (context != null) context.capture(Objects.requireNonNull(execution, "execution"));
    }

    public static boolean isRecording() {
        return CONTEXT.get() != null;
    }

    public static List<String> unconsumedExpectations() {
        AuditContext context = CONTEXT.get();
        return context == null ? List.of() : context.unconsumedExpectations();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    private static AuditContext current() {
        AuditContext context = CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException("API exchange expectations require an active audit scope");
        }
        return context;
    }

    public record CapturedExchange(ApiExecutionResult execution,
                                   ShopApiReportValidation contractValidation) {
        public CapturedExchange {
            Objects.requireNonNull(execution, "execution");
        }
    }

    private record ExpectedExchange(String operationIntent, String description,
                                    ShopApiHttpStatus status) {
        private ExpectedExchange {
            operationIntent = Objects.requireNonNull(operationIntent, "operationIntent").trim();
            if (operationIntent.isBlank()) {
                throw new IllegalArgumentException("operationIntent must not be blank");
            }
            description = Objects.requireNonNull(description, "description").trim();
            if (description.isBlank()) throw new IllegalArgumentException("description must not be blank");
            Objects.requireNonNull(status, "status");
        }

        ShopApiReportValidation evaluate(ApiExecutionResult execution) {
            int actual = execution.response().status();
            return new ShopApiReportValidation("HTTP response contract", List.of(
                    new AtfAssertion("HTTP status", description + " — " + status.name()
                            + " (" + status.code() + ")", Integer.toString(actual),
                            status.matches(actual))), operationIntent);
        }
    }

    private static final class AuditContext {
        private final Consumer<CapturedExchange> recorder;
        private final Deque<ExpectedExchange> expected = new ArrayDeque<>();
        private final List<String> unmatched = new ArrayList<>();

        private AuditContext(Consumer<CapturedExchange> recorder) {
            this.recorder = recorder;
        }

        private void expect(ExpectedExchange exchange) {
            expected.addLast(exchange);
        }

        private void capture(ApiExecutionResult execution) {
            ExpectedExchange contract = expected.pollFirst();
            if (contract == null) {
                unmatched.add("captured without typed contract: "
                        + execution.request().method() + " " + execution.request().path());
                recorder.accept(new CapturedExchange(execution, null));
                return;
            }
            recorder.accept(new CapturedExchange(execution, contract.evaluate(execution)));
        }

        private List<String> unconsumedExpectations() {
            var result = new ArrayList<>(unmatched);
            expected.stream().map(exchange -> "expected exchange was not captured: "
                    + exchange.description()).forEach(result::add);
            return List.copyOf(result);
        }
    }
}
