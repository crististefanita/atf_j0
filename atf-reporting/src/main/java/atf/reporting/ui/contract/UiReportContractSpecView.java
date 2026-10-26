package atf.reporting.ui.contract;

/**
 * Renderer-facing view of a UI report audit plan. Test annotations stay in the
 * consumer module; reporting receives only this immutable capability port.
 */
public interface UiReportContractSpecView {
    boolean has(UiReportCheck check);
}
