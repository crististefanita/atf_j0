package atf.reporting.extent.session;

import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Spark presentation adapter: one 24-hour clock, without changing event times or zones. */
public final class ExtentReportTimestampReporter extends ExtentSparkReporter {
    public static final String TIME_PATTERN = "HH:mm:ss";
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String CATEGORY_TEMPLATE = "spark/macros/attr-common.ftl";
    private static final String CATEGORY_RESOURCE = "/com/aventstack/extentreports/templates/" + CATEGORY_TEMPLATE;
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
        configuration.setTemplateLoader(new MultiTemplateLoader(new TemplateLoader[]{
                override, configuration.getTemplateLoader()}));
        return configuration;
    }

    private static String categoryTemplate() {
        try (InputStream stream = ExtentSparkReporter.class.getResourceAsStream(CATEGORY_RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing Spark category template: " + CATEGORY_RESOURCE);
            String source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            if (!source.contains(VENDOR_CATEGORY_PATTERN)) {
                throw new IllegalStateException("Spark category time expression changed; review the 24-hour adapter");
            }
            return source.replace(VENDOR_CATEGORY_PATTERN, TIME_PATTERN);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read Spark category timestamp template", error);
        }
    }
}
