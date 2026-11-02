package atf.core.http.execution;
import atf.core.http.request.ApiRequest;
import atf.core.http.request.ApiUrlBuilder;
import atf.core.http.response.ApiAttemptOutcome;
import atf.core.http.response.ApiAttemptRecord;
import atf.core.http.response.ApiExecutionException;
import atf.core.http.response.ApiExecutionResult;
import atf.core.http.response.ApiResponse;
import atf.core.http.response.RetryEvent;
import atf.core.http.execution.evidence.ApiExecutionSnapshot;
import atf.core.http.execution.evidence.ApiExecutionCancelledException;
import atf.core.http.transport.ApiTransport;
import atf.core.http.transport.ApiTransportProvenance;
import atf.core.http.transport.RetryPolicy;
import atf.core.reporting.console.AtfConsoleEvent;
import atf.core.reporting.console.AtfConsoleSink;
import atf.core.reporting.console.AtfConsoleDiagnostics;
import atf.core.reporting.contract.AtfReportStatus;
import atf.core.reporting.duration.AtfDurationFormatter;

import java.net.URI;
import atf.core.config.ApiSettings;
import atf.core.config.ApiSettingsSnapshot;
import atf.core.config.ApiTransportMode;
import atf.core.security.SensitiveDataRedactor;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ApiExecutor {
    private final ApiTransport transport;
    private final ApiUrlBuilder urls;
    private final RetryPolicy retryPolicy;
    private final ApiExecutionOptions defaultOptions;
    private final ApiExecutionClock clock;
    private final AtfConsoleSink console;
    private final ApiTransportProvenance provenance;
    private final SensitiveDataRedactor redactor = new SensitiveDataRedactor();

    public ApiExecutor(ApiTransport transport, URI baseUri) {
        this(transport, baseUri, RetryPolicy.none(), ApiExecutionClock.system(), AtfConsoleSink.noOp(),
                ApiTransportProvenance.UNKNOWN);
    }

    public ApiExecutor(ApiTransport transport, URI baseUri, RetryPolicy retryPolicy) {
        this(transport, baseUri, retryPolicy, ApiExecutionClock.system(), AtfConsoleSink.noOp(),
                ApiTransportProvenance.UNKNOWN);
    }

    public ApiExecutor(ApiTransport transport, URI baseUri, AtfConsoleSink console) {
        this(transport, baseUri, RetryPolicy.none(), ApiExecutionClock.system(), console,
                ApiTransportProvenance.UNKNOWN);
    }

    ApiExecutor(ApiTransport transport, URI baseUri, RetryPolicy retryPolicy, ApiExecutionClock clock) {
        this(transport, baseUri, retryPolicy, clock, AtfConsoleSink.noOp(), ApiTransportProvenance.UNKNOWN);
    }

    private ApiExecutor(ApiTransport transport, URI baseUri, RetryPolicy retryPolicy,
                        ApiExecutionClock clock, AtfConsoleSink console,
                        ApiTransportProvenance provenance) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.urls = new ApiUrlBuilder(Objects.requireNonNull(baseUri, "baseUri"));
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy");
        this.defaultOptions = ApiExecutionOptions.defaults();
        this.clock = Objects.requireNonNull(clock, "clock");
        this.console = console == null ? AtfConsoleSink.noOp() : console;
        this.provenance = provenance == null ? ApiTransportProvenance.UNKNOWN : provenance;
    }

    public ApiExecutor(ApiTransport transport, ApiSettings settings) {
        this(transport, Objects.requireNonNull(settings, "settings").snapshot());
    }

    /**
     * Builds an executor from the immutable settings owned by one execution
     * context. The package composition root uses this overload so the
     * executor and the exposed module snapshot cannot diverge.
     */
    public ApiExecutor(ApiTransport transport, ApiSettingsSnapshot settings) {
        this(transport, settings, ApiExecutionClock.system(), AtfConsoleSink.noOp(),
                ApiTransportProvenance.UNKNOWN);
    }

    /** Builds an executor whose report evidence can truthfully identify transport provenance. */
    public ApiExecutor(ApiTransport transport, ApiSettingsSnapshot settings,
                       ApiTransportMode transportMode) {
        this(transport, settings, ApiExecutionClock.system(), AtfConsoleSink.noOp(),
                ApiTransportProvenance.from(transportMode));
    }

    public ApiExecutor(ApiTransport transport, ApiSettingsSnapshot settings, AtfConsoleSink console) {
        this(transport, settings, ApiExecutionClock.system(), console, ApiTransportProvenance.UNKNOWN);
    }

    public ApiExecutor(ApiTransport transport, ApiSettingsSnapshot settings,
                       AtfConsoleSink console, ApiTransportMode transportMode) {
        this(transport, settings, ApiExecutionClock.system(), console,
                ApiTransportProvenance.from(transportMode));
    }

    ApiExecutor(ApiTransport transport, ApiSettingsSnapshot settings, ApiExecutionClock clock) {
        this(transport, settings, clock, AtfConsoleSink.noOp(), ApiTransportProvenance.UNKNOWN);
    }

    private ApiExecutor(ApiTransport transport, ApiSettingsSnapshot settings,
                        ApiExecutionClock clock, AtfConsoleSink console,
                        ApiTransportProvenance provenance) {
        this.transport = Objects.requireNonNull(transport, "transport");
        var snapshot = Objects.requireNonNull(settings, "settings");
        this.urls = new ApiUrlBuilder(snapshot.baseUri());
        this.retryPolicy = RetryPolicy.onStatuses(snapshot.retryStatuses(), snapshot.maxAttempts(), Duration.ZERO);
        this.defaultOptions = new ApiExecutionOptions(snapshot.timeout());
        this.clock = Objects.requireNonNull(clock, "clock");
        this.console = console == null ? AtfConsoleSink.noOp() : console;
        this.provenance = provenance == null ? ApiTransportProvenance.UNKNOWN : provenance;
    }

    public ApiExecutionResult execute(ApiRequest request, ApiSettings settings, CancellationToken cancellationToken) {
        Objects.requireNonNull(settings, "settings");
        return execute(request, new ApiExecutionOptions(settings.timeout()), cancellationToken);
    }

    public ApiExecutionResult execute(ApiRequest request) {
        return execute(request, defaultOptions, CancellationToken.none());
    }

    public ApiExecutionResult execute(ApiRequest request, Duration timeout, CancellationToken cancellationToken) {
        return execute(request, new ApiExecutionOptions(timeout), cancellationToken);
    }

    public ApiExecutionResult execute(ApiRequest request, ApiExecutionOptions options, CancellationToken cancellationToken) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(cancellationToken, "cancellationToken");
        var diagnostics = new AtfConsoleDiagnostics(console);
        String consoleName = redactor.redactText(request.method() + " " + request.path());
        long consoleStarted = clock.nanoTime();
        diagnostics.publish(AtfConsoleEvent.started(AtfConsoleEvent.Scope.REQUEST, consoleName));
        diagnostics.publish(AtfConsoleEvent.detail(AtfConsoleEvent.Scope.REQUEST,
                "method", request.method()));
        var deadline = new ApiExecutionDeadline(clock, options.timeout());
        var retryWaiter = new ApiRetryWaiter(clock, deadline);
        String url = urls.build(request);
        diagnostics.publish(AtfConsoleEvent.detail(AtfConsoleEvent.Scope.REQUEST,
                "url", redactor.redactText(url)));
        if (!request.bodyText().isBlank()) {
            diagnostics.publish(AtfConsoleEvent.code(AtfConsoleEvent.Scope.REQUEST,
                    "request.body", redactor.redactJson(request.bodyText())));
        }
        var retryEvents = new ArrayList<RetryEvent>();
        var attemptHistory = new ArrayList<ApiAttemptRecord>();
        int attempt = 1;
        long attemptStarted = 0;
        boolean activeAttempt = false;
        ApiResponse capturedResponse = null;
        ApiExecutionResult result;
        ApiResponse response;
        try {
            while (true) {
                cancellationToken.throwIfCancelled();
                attemptStarted = clock.nanoTime();
                long remainingNanos = deadline.remainingNanos();
                if (remainingNanos <= 0) {
                    throw new ApiExecutionException("API execution timed out after " + options.timeout(),
                            new TimeoutException("execution deadline exceeded"));
                }
                activeAttempt = true;
                capturedResponse = null;
                CompletableFuture<ApiResponse> pending = transport.send(request, url, request.authProfile());
                try {
                    while (true) {
                        cancellationToken.throwIfCancelled();
                        remainingNanos = deadline.remainingNanos();
                        if (remainingNanos <= 0) {
                            pending.cancel(true);
                            throw new ApiExecutionException("API execution timed out after " + options.timeout(),
                                    new TimeoutException("execution deadline exceeded"));
                        }
                        try {
                            response = pending.get(Math.min(remainingNanos, TimeUnit.MILLISECONDS.toNanos(50)),
                                    TimeUnit.NANOSECONDS);
                            capturedResponse = Objects.requireNonNull(response, "transport response");
                            break;
                        } catch (TimeoutException pollTimeout) {
                            // Poll in short slices so cancellation is observed while transport is pending.
                        }
                    }
                } catch (CancellationException error) {
                    pending.cancel(true);
                    throw error;
                } catch (InterruptedException error) {
                    pending.cancel(true);
                    Thread.currentThread().interrupt();
                    throw new ApiExecutionException("API execution interrupted", error);
                } catch (ExecutionException error) {
                    Throwable cause = error.getCause() == null ? error : error.getCause();
                    var retry = retryPolicy.nextFailure(cause, attempt);
                    if (retry.isEmpty()) {
                        throw new ApiExecutionException("API transport failed", cause);
                    }
                    attemptHistory.add(new ApiAttemptRecord(attempt, 0,
                            Duration.ofNanos(Math.max(0, clock.nanoTime() - attemptStarted)),
                            ApiAttemptOutcome.RETRYABLE_FAILURE, retry.get().reason()));
                    activeAttempt = false;
                    retryEvents.add(retry.get());
                    retryWaiter.await(retry.get().delay(), cancellationToken);
                    attempt++;
                    continue;
                }
                var retry = retryPolicy.next(response, attempt);
                if (retry.isEmpty()) {
                    attemptHistory.add(new ApiAttemptRecord(attempt, response.status(),
                            Duration.ofNanos(Math.max(0, clock.nanoTime() - attemptStarted)),
                            ApiAttemptOutcome.SUCCESS, "final response"));
                    activeAttempt = false;
                    break;
                }
                attemptHistory.add(new ApiAttemptRecord(attempt, response.status(),
                        Duration.ofNanos(Math.max(0, clock.nanoTime() - attemptStarted)),
                        ApiAttemptOutcome.RETRYABLE_RESPONSE, retry.get().reason()));
                activeAttempt = false;
                retryEvents.add(retry.get());
                retryWaiter.await(retry.get().delay(), cancellationToken);
                attempt++;
            }
            result = new ApiExecutionResult(response, attempt,
                    Duration.ofNanos(deadline.elapsedNanos()), retryEvents, attemptHistory, request,
                    provenance);
        } catch (RuntimeException error) {
            ApiAttemptOutcome terminal = terminalOutcome(error);
            ApiResponse terminalResponse = activeAttempt ? capturedResponse : null;
            if (activeAttempt) {
                attemptHistory.add(new ApiAttemptRecord(attempt, terminalResponse == null ? 0 : terminalResponse.status(),
                        Duration.ofNanos(Math.max(0, clock.nanoTime() - attemptStarted)), terminal,
                        terminalResponse == null ? "execution stopped without an HTTP response" : "execution stopped after a captured response"));
            }
            var snapshot = new ApiExecutionSnapshot(request, url, provenance, Duration.ofNanos(deadline.elapsedNanos()),
                    retryEvents, attemptHistory, java.util.Optional.ofNullable(terminalResponse), terminal);
            RuntimeException failure = error instanceof ApiExecutionException execution ? execution.withSnapshot(snapshot)
                    : error instanceof CancellationException cancelled ? new ApiExecutionCancelledException(cancelled, snapshot)
                    : new ApiExecutionException("API execution failed", error, snapshot);
            try {
                diagnostics.publish(AtfConsoleEvent.completed(AtfConsoleEvent.Scope.REQUEST,
                        consoleName, AtfReportStatus.FAIL, elapsedMillis(consoleStarted)));
            } catch (RuntimeException | Error publication) {
                if (publication != failure) failure.addSuppressed(publication);
            }
            diagnostics.attachTo(failure);
            throw failure;
        }
        diagnostics.publish(AtfConsoleEvent.detail(AtfConsoleEvent.Scope.REQUEST,
                "response.status", Integer.toString(response.status())));
        diagnostics.publish(AtfConsoleEvent.detail(AtfConsoleEvent.Scope.REQUEST,
                "response.duration.ms", AtfDurationFormatter.format(response.duration())));
        diagnostics.publish(AtfConsoleEvent.code(AtfConsoleEvent.Scope.REQUEST,
                "response.body", redactor.redactJson(response.body())));
        diagnostics.publish(AtfConsoleEvent.completed(AtfConsoleEvent.Scope.REQUEST,
                consoleName, AtfReportStatus.PASS, elapsedMillis(consoleStarted)));
        return result;
    }

    private static ApiAttemptOutcome terminalOutcome(RuntimeException failure) {
        if (failure instanceof CancellationException) return ApiAttemptOutcome.CANCELLED;
        Throwable cause = failure.getCause();
        if (cause instanceof InterruptedException) return ApiAttemptOutcome.INTERRUPTED;
        if (cause instanceof TimeoutException) return ApiAttemptOutcome.TIMED_OUT;
        return ApiAttemptOutcome.FAILED;
    }
    private long elapsedMillis(long startedAt) {
        return Math.max(0, (clock.nanoTime() - startedAt) / 1_000_000);
    }
}
