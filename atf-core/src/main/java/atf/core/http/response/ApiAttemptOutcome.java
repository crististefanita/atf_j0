package atf.core.http.response;

/** Semantic result of one completed transport attempt. */
public enum ApiAttemptOutcome {
    SUCCESS,
    RETRYABLE_RESPONSE,
    RETRYABLE_FAILURE
}
