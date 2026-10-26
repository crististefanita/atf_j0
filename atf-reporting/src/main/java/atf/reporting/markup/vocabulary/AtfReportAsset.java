package atf.reporting.markup.vocabulary;

import java.util.Arrays;
import java.util.List;

/**
 * Stable vendor asset vocabulary used while making an Extent artifact
 * autonomous. Dynamic asset contents never belong in this enum; only the
 * finite file/type contract does.
 */
public enum AtfReportAsset {
    SPARK_STYLE("spark-style.css", Kind.TEXT, Element.STYLE, "text/css"),
    FONT_AWESOME_STYLE("font-awesome.min.css", Kind.TEXT, Element.STYLE, "text/css"),
    FONT_EOT("fontawesome-webfont.eot", Kind.FONT, null, "application/vnd.ms-fontobject"),
    FONT_WOFF2("fontawesome-webfont.woff2", Kind.FONT, null, "font/woff2"),
    FONT_WOFF("fontawesome-webfont.woff", Kind.FONT, null, "font/woff"),
    FONT_TTF("fontawesome-webfont.ttf", Kind.FONT, null, "font/ttf"),
    FONT_SVG("fontawesome-webfont.svg", Kind.FONT, null, "image/svg+xml"),
    JSON_TREE_SCRIPT("jsontree.js", Kind.TEXT, Element.SCRIPT, "text/javascript"),
    SPARK_SCRIPT("spark-script.js", Kind.TEXT, Element.SCRIPT, "text/javascript"),
    LOGO("logo.png", Kind.IMAGE, null, "image/png");

    public static final String FONT_AWESOME_VERSION = "4.7.0";

    public enum Kind { TEXT, FONT, IMAGE }

    public enum Element { STYLE("style"), SCRIPT("script");
        private final String value;

        Element(String value) { this.value = value; }

        public String value() { return value; }
    }

    private final String fileName;
    private final Kind kind;
    private final Element element;
    private final String mimeType;

    AtfReportAsset(String fileName, Kind kind, Element element, String mimeType) {
        this.fileName = fileName;
        this.kind = kind;
        this.element = element;
        this.mimeType = mimeType;
    }

    public String fileName() { return fileName; }
    public Kind kind() { return kind; }
    /** Compatibility accessor for callers that persist the HTML element name. */
    public String element() { return element == null ? null : element.value(); }
    public Element elementType() { return element; }
    public String mimeType() { return mimeType; }

    public static List<AtfReportAsset> textAssets() {
        return Arrays.stream(values()).filter(asset -> asset.kind == Kind.TEXT).toList();
    }

    public static List<AtfReportAsset> fontAssets() {
        return Arrays.stream(values()).filter(asset -> asset.kind == Kind.FONT).toList();
    }
}
