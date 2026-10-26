package atf.autoex.flow.checkout;

import atf.autoex.business.invoice.AutoExInvoiceSnapshot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Converts the downloaded artifact into a snapshot; it does not validate or publish a verdict. */
public final class AutoExInvoiceReader {
    private AutoExInvoiceReader() { }

    public static AutoExInvoiceSnapshot read(String fileName, Path path) {
        boolean available = path != null && Files.isRegularFile(path);
        try {
            return new AutoExInvoiceSnapshot(fileName, available ? Files.readString(path) : "", available);
        } catch (IOException failure) {
            throw new UncheckedIOException("Cannot read the downloaded invoice", failure);
        }
    }
}
