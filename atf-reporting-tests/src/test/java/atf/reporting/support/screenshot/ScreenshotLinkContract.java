package atf.reporting.support.screenshot;

import java.util.Base64;
import org.jsoup.Jsoup;
import static org.junit.jupiter.api.Assertions.*;

/** Checks published evidence links, never selector literals embedded in scripts. */
public final class ScreenshotLinkContract {
    private ScreenshotLinkContract() { }

    public static void assertSingleOriginal(String html, String png, String caption) {
        var links = Jsoup.parse(html).select("a[data-atf-screenshot=base64]");
        assertEquals(1, links.size(), "One semantic link owns the deduplicated capture");
        var link = links.first();
        String prefix = "data:image/png;base64,";
        assertTrue(link.attr("href").startsWith(prefix), "Evidence remains an embedded PNG");
        assertArrayEquals(Base64.getDecoder().decode(png),
                Base64.getDecoder().decode(link.attr("href").substring(prefix.length())), "Original captured bytes remain intact");
        assertEquals("Open " + caption, link.attr("aria-label"), "Caption belongs to the original checkpoint");
    }
}
