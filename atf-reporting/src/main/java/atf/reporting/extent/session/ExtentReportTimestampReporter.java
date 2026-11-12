package atf.reporting.extent.session;

import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Spark presentation adapter for clock formats and context-specific title escaping. */
public final class ExtentReportTimestampReporter extends ExtentSparkReporter {
    public static final String TIME_PATTERN = "HH:mm:ss";
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String CATEGORY_TEMPLATE = "spark/macros/attr-common.ftl";
    private static final String TIMELINE_TEMPLATE = "spark/partials/dashboard.ftl";
    private static final String TEST_TEMPLATE = "spark/partials/test.ftl";
    private static final String NODE_TEMPLATE = "spark/macros/recurse_nodes.ftl";
    private static final String TIMELINE_KEY = "\"${t.name}\":";
    private static final String VENDOR_CATEGORY_PATTERN = "HH:mm:ss a";

    public ExtentReportTimestampReporter(String filePath) {
        super(filePath);
        config().setTimeStampFormat(DATE_TIME_PATTERN);
    }

    @Override
    protected Configuration createFreemarkerConfig(String basePackagePath, String encoding) {
        Configuration configuration = super.createFreemarkerConfig(basePackagePath, encoding);
        configuration.setTimeFormat(TIME_PATTERN);
        configuration.setDateTimeFormat(DATE_TIME_PATTERN);
        // Spark's category macro ignores timeStampFormat and hardcodes a mixed 24h/AM-PM pattern.
        // Adapt that single expression in memory; keep the vendor structure and timezone intact.
        var override = new StringTemplateLoader();
        override.putTemplate(CATEGORY_TEMPLATE, categoryTemplate());
        override.putTemplate(TIMELINE_TEMPLATE, timelineTemplate());
        override.putTemplate(TEST_TEMPLATE, htmlText(template(TEST_TEMPLATE), "test.name", "test.description"));
        override.putTemplate(NODE_TEMPLATE, htmlText(template(NODE_TEMPLATE), "node.name", "node.description"));
        configuration.setTemplateLoader(new MultiTemplateLoader(new TemplateLoader[]{
                override, configuration.getTemplateLoader()}));
        return configuration;
    }

    private static String categoryTemplate() {
        String source = template(CATEGORY_TEMPLATE);
        if (!source.contains(VENDOR_CATEGORY_PATTERN)) {
            throw new IllegalStateException("Spark category time expression changed; review the 24-hour adapter");
        }
        return htmlText(source.replace(VENDOR_CATEGORY_PATTERN, TIME_PATTERN), "test.name", "test.getFullName()");
    }

    private static String timelineTemplate() {
        String source = template(TIMELINE_TEMPLATE);
        if (!source.contains(TIMELINE_KEY)) {
            throw new IllegalStateException("Spark timeline key expression changed; review the JSON-string adapter");
        }
        return source.replace(TIMELINE_KEY, "\"${t.name?json_string?replace('<', r'\\u003c')}\":");
    }

    private static String htmlText(String source, String... expressions) {
        for (String expression : expressions) {
            String reference = "${" + expression + "}";
            if (!source.contains(reference)) {
                throw new IllegalStateException("Spark title expression changed: " + reference);
            }
            source = source.replace(reference, "${" + expression + "?html}");
        }
        return source;
    }

    private static String template(String name) {
        String resource = "/com/aventstack/extentreports/templates/" + name;
        try (InputStream stream = ExtentSparkReporter.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Missing Spark template: " + resource);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read Spark presentation template: " + name, error);
        }
    }
}
