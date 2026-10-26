package atf.reporting.inventory;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import java.util.Collection;

/** Minimal assertion port; no test framework or ATF runtime dependency. */
public interface ValidationSink {
    Scope startValidation(String title);
    interface Scope extends AutoCloseable {
        void assertions(Collection<AtfAssertion> assertions);
        void complete(AtfReportStatus status);
        @Override void close();
    }
}
