package atf.reporting.extent.session;

import com.aventstack.extentreports.ExtentTest;

/** Package-local fault seam for proving start rollback without changing the consumer API. */
interface LifecycleStartProbe {
    void beforeScenarioSemantic();
    void beforePhaseSemantic(ExtentTest phase);

    static LifecycleStartProbe noOp() {
        return new LifecycleStartProbe() {
            @Override public void beforeScenarioSemantic() { }
            @Override public void beforePhaseSemantic(ExtentTest phase) { }
        };
    }
}
