package atf.autoex.business.invoice;

import java.util.Objects;

/** Download facts, independent of expected customer/amount and of the browser handle. */
public record AutoExInvoiceSnapshot(String fileName, String contents, boolean available) {
    public AutoExInvoiceSnapshot {
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(contents, "contents");
    }
}
