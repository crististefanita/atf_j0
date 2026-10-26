package atf.reporting.html.script;

/**
 * Renderer-owned JavaScript asset boundary for the fallback HTML report.
 *
 * <p>The report session composes assets through this small port instead of
 * embedding executable JavaScript in the lifecycle renderer. This keeps the
 * document model and the browser behaviour independently replaceable.</p>
 */
@FunctionalInterface
public interface HtmlReportScript {
    String source();
}
