package atf.reporting.extent.session;

import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Spark presentation adapter: native clock formats and encoded timeline keys, without changing captured values. */
public final class ExtentReportTimestampReporter extends ExtentSparkReporter {
    public static final String TIME_PATTERN = "HH:mm:ss";
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String CATEGORY_TEMPLATE = "spark/macros/attr-common.ftl";
    private static final String TIMELINE_TEMPLATE = "spark/partials/dashboard.ftl";
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
        configuration.setTemplateLoader(new MultiTemplateLoader(new TemplateLoader[]{
                override, configuration.getTemplateLoader()}));
        return configuration;
    }

    private static String categoryTemplate() {
        String source = template(CATEGORY_TEMPLATE);
        if (!source.contains(VENDOR_CATEGORY_PATTERN)) {
            throw new IllegalStateException("Spark category time expression changed; review the 24-hour adapter");
        }
        return source.replace(VENDOR_CATEGORY_PATTERN, TIME_PATTERN);
    }

    private static String timelineTemplate() {
        String source = template(TIMELINE_TEMPLATE);
        if (!source.contains(TIMELINE_KEY)) {
            throw new IllegalStateException("Spark timeline key expression changed; review the JSON-string adapter");
        }
        return source.replace(TIMELINE_KEY, "\"${t.name?json_string}\":");
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
