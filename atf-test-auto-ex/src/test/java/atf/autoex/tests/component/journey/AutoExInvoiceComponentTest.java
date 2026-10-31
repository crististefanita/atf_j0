package atf.autoex.tests.component.journey;

import atf.autoex.business.invoice.AutoExInvoiceSnapshot;
import atf.autoex.business.invoice.AutoExInvoiceValidator;
import atf.autoex.flow.checkout.AutoExInvoiceReader;
import atf.autoex.support.checkout.AutoExInvoiceAssertions;
import atf.reporting.contract.AtfAssertion;
import atf.autoex.tests.extension.AutoExComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.CharacterCodingException;
import java.util.List;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

@AutoExComponentTest
class AutoExInvoiceComponentTest {
    private static final String CUSTOMER = "ATF Customer";
    private static final String TOTAL = "Rs. 1000";
    private static final String CONTENTS = "Hi ATF Customer, Your total purchase amount is 1000.00. Thank you.";
    @TempDir Path temporary;

    @Test
    void artifactReaderAndPureValidatorPreserveAllFourPublishedChecks() throws Exception {
        Path file = temporary.resolve("received-invoice.txt");
        Files.writeString(file, CONTENTS);
        var snapshot = AutoExInvoiceReader.read("invoice.txt", file);
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
        var snapshot = AutoExInvoiceReader.read("invoice.txt", null);
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
                () -> AutoExInvoiceReader.read("invoice.txt", invalid));
        assertTrue(failure.getMessage().contains("Cannot read the downloaded invoice"));
        assertTrue(failure.getCause() instanceof CharacterCodingException);
    }

    @ParameterizedTest(name = "malformed invoice amount: {0}")
    @ValueSource(strings = {"1000.00oops", "1000.00.99", "1000,99", "1,000.00", "1000e0",
            "+1000", "-1000", "1000.00..", "1000.00USD", "1000_00", "1 000.00", "1000 99", "1000.00 USD"})
    void completeMalformedTokenFailsOnlyTheAmountAndSurvivesBothReportingAdapters(String token) throws Exception {
        var invoice = invoiceWithAmount(token);
        var checks = AutoExInvoiceValidator.validate(CUSTOMER, TOTAL, invoice);
        assertEquals(List.of("Invoice amount"), checks.stream().filter(check -> !check.passed())
                .map(AutoExInvoiceValidator.Check::label).toList(), "An available file and correct customer cannot hide an invalid amount");
        assertEquals(new AutoExInvoiceValidator.Check("Invoice amount", "1000", token, false), checks.get(2),
                "The complete observed token remains diagnostic evidence, without numeric-prefix repair");
        var assertions = AutoExInvoiceAssertions.validate(CUSTOMER, TOTAL, invoice);
        assertEquals(new AtfAssertion("Invoice amount", "1000", token, false), assertions.get(2),
                "The reporting adapter preserves the business FAIL and its exact amount");
        assertEquals(assertions.subList(0, 3), AutoExInvoiceAssertions.validate(CUSTOMER, TOTAL,
                invoice.fileName(), invoice.contents()), "Both public adapters preserve the same content checks");
    }

    @ParameterizedTest(name = "valid invoice amount: {0}")
    @CsvSource(value = {"1000|1000", "1000.0|1000.0", "1000.0000|1000.0000", "Rs. 1000.00|1000.00",
            "Rs.1000.00|1000.00", "rs. 1000.00|1000.00", "0001000.00|0001000.00"}, delimiter = '|')
    void completeDecimalsRetainTheirRepresentationAndCompareByValue(String observed, String amount) throws Exception {
        var invoice = invoiceWithAmount(observed);
        var checks = AutoExInvoiceValidator.validate(CUSTOMER, TOTAL, invoice);
        assertEquals(new AutoExInvoiceValidator.Check("Invoice amount", "1000", amount, true), checks.get(2),
                "Scale and the optional observed currency prefix do not change the independent expected value");
        assertTrue(checks.stream().allMatch(AutoExInvoiceValidator.Check::passed), "All four invoice checks remain valid");
    }

    @ParameterizedTest(name = "amount terminator: [{0}]")
    @ValueSource(strings = {"", " ", "\n", ". Thank you.", "\r\nThank you."})
    void endOfArtifactWhitespaceAndSentencePunctuationKeepTheCompleteAmount(String suffix) {
        var invoice = new AutoExInvoiceSnapshot("invoice.txt",
                "Hi ATF Customer, Your total purchase amount is 1000.00" + suffix, true);
        var checks = AutoExInvoiceValidator.validate(CUSTOMER, TOTAL, invoice);
        assertEquals(new AutoExInvoiceValidator.Check("Invoice amount", "1000", "1000.00", true), checks.get(2));
        assertTrue(checks.stream().allMatch(AutoExInvoiceValidator.Check::passed));
    }

    private AutoExInvoiceSnapshot invoiceWithAmount(String token) throws Exception {
        Path file = temporary.resolve("received-invoice.txt");
        Files.writeString(file, CONTENTS.replace("1000.00", token));
        return AutoExInvoiceReader.read("invoice.txt", file);
    }
}
