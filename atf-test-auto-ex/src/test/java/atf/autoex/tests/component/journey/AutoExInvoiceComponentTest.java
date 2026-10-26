package atf.autoex.tests.component.journey;

import atf.autoex.business.invoice.AutoExInvoiceSnapshot;
import atf.autoex.business.invoice.AutoExInvoiceValidator;
import atf.autoex.flow.checkout.AutoExInvoiceReader;
import atf.autoex.support.checkout.AutoExInvoiceAssertions;
import atf.reporting.contract.AtfAssertion;
import atf.testkit.AtfComponentTest;
import com.microsoft.playwright.Download;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.UncheckedIOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.CharacterCodingException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class AutoExInvoiceComponentTest {
    private static final String CUSTOMER = "ATF Customer";
    private static final String TOTAL = "Rs. 1000";
    private static final String CONTENTS = "Hi ATF Customer, Your total purchase amount is 1000.00. Thank you.";
    @TempDir Path temporary;

    @Test
    void artifactReaderAndPureValidatorPreserveAllFourPublishedChecks() throws Exception {
        Path file = temporary.resolve("received-invoice.txt");
        Files.writeString(file, CONTENTS);
        var snapshot = AutoExInvoiceReader.read(download("invoice.txt", file));
        assertEquals(new AutoExInvoiceSnapshot("invoice.txt", CONTENTS, true), snapshot);
        assertTrue(AutoExInvoiceValidator.validate(CUSTOMER, TOTAL, snapshot).stream()
                .allMatch(AutoExInvoiceValidator.Check::passed));
        var assertions = AutoExInvoiceAssertions.validate(CUSTOMER, TOTAL, snapshot);
        assertEquals(List.of("Invoice file", "Invoice customer", "Invoice amount", "Invoice download"),
                assertions.stream().map(AtfAssertion::name).toList());
        assertTrue(assertions.stream().allMatch(AtfAssertion::passed));
    }

    @Test
    void missingArtifactCannotBeCompletedFromTheExpectedIdentityAndAmount() {
        var snapshot = AutoExInvoiceReader.read(download("invoice.txt", null));
        assertFalse(snapshot.available());
        assertEquals("", snapshot.contents());
        var checks = AutoExInvoiceValidator.validate(CUSTOMER, TOTAL, snapshot);
        assertEquals(List.of("Invoice customer", "Invoice amount", "Invoice download"), checks.stream()
                .filter(check -> !check.passed()).map(AutoExInvoiceValidator.Check::label).toList());
    }

    @Test
    void wrongCustomerAmountOrFilenameRemainsAFailureEvenWithAnAvailableFile() {
        for (var invoice : List.of(new AutoExInvoiceSnapshot("invoice.txt", CONTENTS.replace(CUSTOMER, CUSTOMER + "2"), true),
                new AutoExInvoiceSnapshot("invoice.txt", CONTENTS.replace("1000.00", "10000.00"), true),
                new AutoExInvoiceSnapshot("error.html", CONTENTS, true))) {
            assertTrue(AutoExInvoiceValidator.validate(CUSTOMER, TOTAL, invoice).stream().anyMatch(check -> !check.passed()));
        }
    }

    @Test
    void unreadableArtifactRetainsItsIoCauseRatherThanPublishingAnEmptySuccessfulInvoice() throws Exception {
        Path invalid = temporary.resolve("invalid-utf8.txt");
        Files.write(invalid, new byte[]{(byte) 0xC3});
        var failure = assertThrows(UncheckedIOException.class,
                () -> AutoExInvoiceReader.read(download("invoice.txt", invalid)));
        assertTrue(failure.getMessage().contains("Cannot read the downloaded invoice"));
        assertTrue(failure.getCause() instanceof CharacterCodingException);
    }

    private static Download download(String name, Path file) {
        return (Download) Proxy.newProxyInstance(Download.class.getClassLoader(), new Class<?>[]{Download.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "suggestedFilename" -> name;
                    case "path" -> file;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
