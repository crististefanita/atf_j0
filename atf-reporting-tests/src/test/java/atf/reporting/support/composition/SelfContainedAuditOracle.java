package atf.reporting.support.composition;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import static org.junit.jupiter.api.Assertions.*;

/** Independent original-owner oracle; no expected value comes from a renderer snapshot. */
public final class SelfContainedAuditOracle {
    private SelfContainedAuditOracle() { }
    public static List<Element> owners(Document doc, boolean extent) {
        return doc.select(extent ? "#test-view .test-list-item > .test-item" : "#tests article.scenario");
    }
    public static List<Element> rows(Element owner, boolean extent) {
        return owner.select(extent ? "table.helm-validation-table tr" : "table.assertions tr").stream()
                .filter(row -> row.childrenSize() == 4 && row.children().stream().allMatch(cell -> cell.tagName().equals("td"))).toList();
    }
    public static void verify(Path file, boolean extent, boolean ui, boolean combined) throws Exception {
        String html = Files.readString(file); Document doc = Jsoup.parse(html);
        assertFalse(html.contains(SelfContainedAuditFixture.CANARY), "No secret in visible or hidden report channels");
        assertTrue(doc.select("a[href^=file:]").isEmpty(), "All evidence is inline, not a local file link");
        assertTrue(doc.select("a[href~=\\.(xml|json|properties|log|zip)(?:$|[#?])]").isEmpty(), "No external evidence dependency");
        var owners = owners(doc, extent); assertEquals(combined ? 2 : 1, owners.size());
        for (int index = 0; index < owners.size(); index++) {
            var owner = owners.get(index); var bodies = owner.select("[data-atf-node-role=observation] pre.atf-observation-body");
            assertEquals(2, bodies.size(), "Both full values belong to the original invocation");
            var captured = new ObjectMapper().readTree(bodies.getFirst().wholeText());
            assertEquals(SelfContainedAuditFixture.id(index), captured.path("id").asText());
            assertEquals("9007199254740993", captured.path("large").asText());
            assertEquals(SelfContainedAuditFixture.payload(index), captured.path("payload").asText());
            assertEquals("***REDACTED***", captured.path("token").asText());
            assertEquals(SelfContainedAuditFixture.narrative(index), bodies.getLast().wholeText());
            var timing = owner.selectFirst("[data-atf-execution-duration-ms]"); assertNotNull(timing);
            assertEquals(Integer.toString(2345 + index), timing.attr("data-atf-execution-duration-ms"));
            var rows = rows(owner, extent); assertEquals(2, rows.size(), "Exactly the two evaluated comparisons");
            assertEquals("Recorded count", rows.getFirst().child(0).text());
            assertEquals("41", rows.getFirst().child(1).text());
            assertEquals(index == 0 ? "41" : "42", rows.getFirst().child(2).text());
            assertEquals(extent ? (index == 0 ? "✅" : "❌") : (index == 0 ? "Pass" : "Fail"), rows.getFirst().child(3).text());
            assertEquals("Stable input identity", rows.getLast().child(0).text());
            assertEquals(SelfContainedAuditFixture.id(index), rows.getLast().child(1).text());
            assertEquals(SelfContainedAuditFixture.id(index), rows.getLast().child(2).text());
            assertEquals(extent ? "✅" : "Pass", rows.getLast().child(3).text());
            var images = owner.select("a[data-atf-screenshot=base64]"); assertEquals(ui ? 1 : 0, images.size());
            if (ui) assertEquals("data:image/png;base64," + SelfContainedAuditFixture.png(index), images.getFirst().attr("href"));
        }
    }
}
