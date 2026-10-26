package atf.core.lifecycle;

/**
 * Explicit skip signal for a lifecycle phase. A skip is not an exception
 * equivalent to failure: teardown still runs and the scenario keeps SKIP as
 * its outcome unless a later phase fails.
 */
public class ApiScenarioSkipException extends RuntimeException {
    public ApiScenarioSkipException(String message) {
        super(message);
    }
}
