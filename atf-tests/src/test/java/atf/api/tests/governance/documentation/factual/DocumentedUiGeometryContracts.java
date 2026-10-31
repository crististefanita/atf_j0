package atf.api.tests.governance.documentation.factual;

import atf.ui.config.UiBrowserConfiguration;
import atf.ui.config.geometry.UiViewportScale;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Only selected numeric claims: live YAML and the executable UI model remain the authorities. */
final class DocumentedUiGeometryContracts {
    static final String UI = "atf-ui/README.md";
    static final List<String> DOCUMENTS = List.of(UI, "atf-test-auto-ex/README.md", "atf-test-practice-shop/README.md");
    private static final String SCALE_SOURCE = "atf-ui/src/main/java/atf/ui/config/geometry/UiViewportScale.java";
    private static final String DEFAULT_SOURCE = "atf-ui/src/main/java/atf/ui/config/UiBrowserConfiguration.java";
    private static final Pattern DEFAULT = Pattern.compile("(?m)^Implicitul generic al `zoomPercent` este `(\\d+)`");
    private static final Pattern EXAMPLE = Pattern.compile(
            "(?m)^\\| ieșire cerută `(\\d+) × (\\d+)`, zoom `(\\d+)` \\| viewport CSS `(\\d+) × (\\d+)`, DPR `([0-9.]+)` \\|$");
    private static final Pattern DELIVERED = Pattern.compile(
            "(?m)^\\| YAML livrat \\| `(\\d+) × (\\d+)` \\| `(\\d+)` \\| `(\\d+) × (\\d+)` \\| `([0-9.]+)` \\|$");

    private DocumentedUiGeometryContracts() { }

    static List<String> inspect(Path root, String relative, String document) throws Exception {
        if (!DOCUMENTS.contains(relative)) throw new IllegalArgumentException("Unselected document: " + relative);
        String prose = withoutFences(document);
        var failures = new ArrayList<String>();
        if (relative.equals(UI)) {
            Matcher defaults = single(prose, DEFAULT, relative, "generic zoom default", DEFAULT_SOURCE, failures);
            if (defaults != null && new BigDecimal(defaults.group(1)).compareTo(BigDecimal.valueOf(UiBrowserConfiguration.defaults().zoomPercent())) != 0) {
                failures.add(diagnostic(relative, prose, defaults.start(), "generic zoom default", defaults.group(1),
                        Integer.toString(UiBrowserConfiguration.defaults().zoomPercent()), DEFAULT_SOURCE));
            }
        }
        boolean example = relative.equals(UI);
        String yaml = example ? null : relative.substring(0, relative.indexOf('/')) + "/src/test/resources/"
                + (relative.contains("auto-ex") ? "atf-auto-ex.yaml" : "atf-practice-shop.yaml");
        String source = example ? SCALE_SOURCE : yaml + " -> " + SCALE_SOURCE;
        Matcher row = single(prose, example ? EXAMPLE : DELIVERED, relative, "UI geometry", source, failures);
        if (row == null) return List.copyOf(failures);
        try {
            Geometry declared = Geometry.read(row);
            JsonNode configured = example ? null : new ObjectMapper(new YAMLFactory()).readTree(root.resolve(yaml).toFile())
                    .at(relative.contains("auto-ex") ? "/automationExercise/browser" : "/practiceShop");
            int width = example ? declared.width : integer(configured, "/viewport/width");
            int height = example ? declared.height : integer(configured, "/viewport/height");
            int zoom = example ? declared.zoom : integer(configured, "/zoomPercent");
            Geometry observed = Geometry.from(width, height, zoom);
            if (!declared.equals(observed)) failures.add(diagnostic(relative, prose, row.start(),
                    "UI geometry", declared.toString(), observed.toString(), source));
        } catch (IllegalArgumentException invalid) {
            failures.add(diagnostic(relative, prose, row.start(), "UI geometry", "invalid numeric declaration or source",
                    invalid.getMessage(), source));
        }
        return List.copyOf(failures);
    }

    private static int integer(JsonNode source, String pointer) {
        JsonNode value = source.at(pointer);
        if (!value.isIntegralNumber() || !value.canConvertToInt()) throw new IllegalArgumentException("Expected integer at " + pointer);
        return value.intValue();
    }

    private static Matcher single(String prose, Pattern pattern, String document, String claim, String source, List<String> failures) {
        Matcher match = pattern.matcher(prose);
        if (!match.find()) {
            failures.add(diagnostic(document, prose, 0, claim, "missing or malformed declaration", "one declaration", source));
            return null;
        }
        int first = match.start();
        if (match.find()) {
            failures.add(diagnostic(document, prose, match.start(), claim, "duplicate declaration", "one declaration", source));
            return null;
        }
        match.reset(); match.find(first);
        return match;
    }

    /** Preserve line numbers, and never let a code sample satisfy a missing prose claim. */
    private static String withoutFences(String document) {
        StringBuilder prose = new StringBuilder();
        char marker = 0; int opening = 0;
        for (String line : document.split("\\R", -1)) {
            String value = line.stripLeading(); int count = 0;
            char candidate = value.isEmpty() ? 0 : value.charAt(0);
            if (candidate == '`' || candidate == '~') while (count < value.length() && value.charAt(count) == candidate) count++;
            boolean boundary = count >= 3 && (marker == 0 || candidate == marker && count >= opening && value.substring(count).isBlank());
            boolean hidden = marker != 0 || boundary;
            if (boundary) { if (marker == 0) { marker = candidate; opening = count; } else marker = 0; }
            prose.append(hidden ? "" : line).append('\n');
        }
        return prose.toString();
    }

    private static String diagnostic(String document, String prose, int offset, String claim, String declared, String observed, String source) {
        int line = 1 + (int) prose.substring(0, offset).chars().filter(value -> value == '\n').count();
        return "DOC-FACT " + document + ":" + line + " " + claim + ": declared=" + declared + "; observed=" + observed + "; source=" + source;
    }

    private record Geometry(int width, int height, int zoom, int cssWidth, int cssHeight, BigDecimal dpr) {
        Geometry { dpr = dpr.stripTrailingZeros(); }
        static Geometry read(Matcher row) {
            return new Geometry(Integer.parseInt(row.group(1)), Integer.parseInt(row.group(2)), Integer.parseInt(row.group(3)),
                    Integer.parseInt(row.group(4)), Integer.parseInt(row.group(5)), new BigDecimal(row.group(6)));
        }
        static Geometry from(int width, int height, int zoom) {
            var defaults = UiBrowserConfiguration.defaults();
            var configuration = new UiBrowserConfiguration(defaults.baseUrl(), defaults.browser(), defaults.headless(), defaults.timeout(),
                    defaults.navigationTimeout(), width, height, zoom, defaults.screenshotOnFailure(), defaults.retryPolicy(), defaults.networkInspection());
            var scale = new UiViewportScale(configuration);
            return new Geometry(width, height, zoom, scale.layoutWidth(), scale.layoutHeight(), BigDecimal.valueOf(scale.deviceScaleFactor()));
        }
    }
}
