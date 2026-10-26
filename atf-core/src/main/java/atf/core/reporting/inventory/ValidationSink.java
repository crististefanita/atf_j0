package atf.core.reporting.inventory;
import atf.core.reporting.contract.AtfAssertion;
import atf.core.reporting.contract.AtfReportStatus;

import java.util.Collection;

/**
 * Narrow reporting port exposed to validation code.
 *
 * <p>The validation module can publish assertion evidence without depending
 * on a request implementation, Extent, HTML, or any other renderer.</p>
 */
public interface ValidationSink {
    Scope startValidation(String title);

    interface Scope extends AutoCloseable {
        void assertions(Collection<AtfAssertion> assertions);
        void complete(AtfReportStatus status);

        @Override
        void close();
    }
}
