package atf.reporting.observation;

import atf.reporting.support.composition.SelfContainedAuditFixture;
import atf.reporting.support.composition.SelfContainedAuditOracle;
import atf.testkit.AtfComponentTest;
import java.nio.file.*;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

/** Complete inline evidence for standalone and combined component/UI-shaped reports. */
@AtfComponentTest
class SelfContainedAuditComponentTest {
    static Stream<Arguments> matrix() {
        return Stream.of(false, true).flatMap(extent -> Stream.of(false, true)
                .flatMap(ui -> Stream.of(false, true).map(combined -> Arguments.of(extent, ui, combined))));
    }
    @ParameterizedTest @MethodSource("matrix")
    void originalFactsRemainCompleteWithoutOpeningAnyArtifact(boolean extent, boolean ui, boolean combined) throws Exception {
        Path report = SelfContainedAuditFixture.publish(extent, ui, combined, "component");
        SelfContainedAuditOracle.verify(report, extent, ui, combined);
        System.out.println("SELF_CONTAINED_AUDIT=" + report);
    }
    @ParameterizedTest @MethodSource("styles")
    void missingOrReassociatedEvidenceCannotPassTheIndependentOracle(boolean extent, boolean ui) throws Exception {
        Path report = SelfContainedAuditFixture.publish(extent, ui, true, "negative");
        SelfContainedAuditOracle.verify(report, extent, ui, true);
        String original = Files.readString(report);
        var doc = Jsoup.parse(original, "", Parser.htmlParser().setTrackPosition(true));
        var owners = SelfContainedAuditOracle.owners(doc, extent);
        var first = owners.getFirst().selectFirst("pre.atf-observation-body");
        var second = owners.getLast().selectFirst("pre.atf-observation-body");
        String swapped = inner(original, second, content(original, first));
        swapped = inner(swapped, first, content(original, second));
        refuse(report, "cross-owner", original, swapped, extent, ui);
        var last = SelfContainedAuditOracle.rows(owners.getLast(), extent).getLast();
        refuse(report, "last-row", original, replace(original, last.sourceRange().start().pos(), last.endSourceRange().end().pos(), ""), extent, ui);
        var verdict = SelfContainedAuditOracle.rows(owners.getLast(), extent).getFirst().child(3);
        refuse(report, "false-pass", original, inner(original, verdict, extent ? "✅" : "Pass"), extent, ui);
        var timing = owners.getFirst().selectFirst("[data-atf-execution-duration-ms]");
        String tag = original.substring(timing.sourceRange().start().pos(), timing.sourceRange().end().pos());
        refuse(report, "timing", original, replace(original, timing.sourceRange().start().pos(), timing.sourceRange().end().pos(),
                tag.replace("data-atf-execution-duration-ms=\"2345\"", "data-atf-execution-duration-ms=\"9999\"")), extent, ui);
        int end = original.lastIndexOf("</body>"); assertTrue(end > 0);
        refuse(report, "file-link", original, replace(original, end, end, "<a href='file:///missing/receipt.xml'>Read evidence elsewhere</a>"), extent, ui);
        if (ui) {
            var image = owners.getLast().selectFirst("a[data-atf-screenshot=base64]");
            tag = original.substring(image.sourceRange().start().pos(), image.sourceRange().end().pos());
            refuse(report, "png-owner", original, replace(original, image.sourceRange().start().pos(), image.sourceRange().end().pos(),
                    tag.replace(SelfContainedAuditFixture.png(1), SelfContainedAuditFixture.png(0))), extent, true);
        }
        SelfContainedAuditOracle.verify(report, extent, ui, true);
    }
    static Stream<Arguments> styles() { return Stream.of(false, true).flatMap(extent -> Stream.of(false, true).map(ui -> Arguments.of(extent, ui))); }
    private static String content(String raw, Element element) {
        return raw.substring(element.sourceRange().end().pos(), element.endSourceRange().start().pos());
    }
    private static String inner(String raw, Element element, String value) {
        return replace(raw, element.sourceRange().end().pos(), element.endSourceRange().start().pos(), value);
    }
    private static String replace(String raw, int start, int end, String value) {
        assertTrue(start >= 0 && end >= start && end <= raw.length(), "Tracked raw HTML range");
        return raw.substring(0, start) + value + raw.substring(end);
    }
    private static void refuse(Path report, String kind, String original, String altered, boolean extent, boolean ui) throws Exception {
        assertNotEquals(original, altered, kind + " actually changes the intended evidence");
        Path mutant = report.resolveSibling("mutant-" + kind + "-" + report.getFileName()); Files.writeString(mutant, altered);
        assertThrows(AssertionError.class, () -> SelfContainedAuditOracle.verify(mutant, extent, ui, true), kind);
    }
}
