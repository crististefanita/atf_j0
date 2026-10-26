package atf.reporting.evidence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Static cross-report gate for the shared evidence shell.
 *
 * <p>Browser computed-style checks still belong to the ATF Debug matrix, but
 * this gate prevents publishing a UI/API pair whose embedded stylesheet has
 * drifted before a browser is opened.</p>
 */
public final class ReportEvidenceStyleContract {
    private static final Set<String> SHARED_TOKENS = Set.of(
            "--atf-evidence-status-column-width",
            "--atf-evidence-summary-padding",
            "--atf-evidence-summary-height",
            "--atf-evidence-radius",
            "--atf-evidence-key-column-width");

    private ReportEvidenceStyleContract() {
    }

    public static Comparison compare(Path uiReport, Path apiReport) {
        Snapshot ui = inspect(uiReport, true);
        Snapshot api = inspect(apiReport, false);
        Map<String, Boolean> crossChecks = new LinkedHashMap<>();
        SHARED_TOKENS.forEach(token -> crossChecks.put(
                "shared token " + token,
                !ui.tokens().getOrDefault(token, "").isBlank()
                        && ui.tokens().get(token).equals(api.tokens().get(token))));
        crossChecks.put("shared local table overflow",
                has(ui, "local action overflow") && has(api, "local action overflow"));
        crossChecks.put("shared sibling disclosure policy",
                has(ui, "no API sibling top divider") && has(api, "no API sibling top divider"));
        return new Comparison(ui, api, crossChecks);
    }

    public static Snapshot inspect(Path report, boolean uiExpected) {
        Objects.requireNonNull(report, "report");
        try {
            return inspectHtml(report, Files.readString(report, StandardCharsets.UTF_8), uiExpected);
        } catch (IOException error) {
            return Snapshot.failed(report, error.toString());
        }
    }

    public static Snapshot inspectHtml(Path report, String html, boolean uiExpected) {
        String source = Objects.requireNonNull(html, "html");
        String body = reportBody(source);
        Map<String, Boolean> checks = new LinkedHashMap<>();
        Map<String, String> tokens = new LinkedHashMap<>();
        SHARED_TOKENS.forEach(token -> tokenValue(source, token).ifPresent(value -> tokens.put(token, value)));
        checks.put("status token", tokens.containsKey("--atf-evidence-status-column-width"));
        checks.put("summary token", tokens.containsKey("--atf-evidence-summary-padding"));
        checks.put("fluid details", source.contains("min-width:0!important"));
        checks.put("local action overflow", source.contains("overscroll-behavior:contain!important")
                || source.contains("overscroll-behavior-x:contain!important"));
        checks.put("keyboard evidence state", source.contains("summary.setAttribute('aria-expanded','false')")
                && source.contains("details.open?'true':'false'"));
        checks.put("no API sibling top divider", !source.contains(
                "details.step-detail.response-evidence>summary{border-top:1px solid var(--atf-border)!important}"));
        checks.put("specialized evidence", hasClassElement(body, "table", "helm-evidence-table")
                && hasClassElement(body, "table", "helm-validation-table"));
        if (uiExpected) {
            checks.put("UI action zone", hasClassElement(body, "details", "atf-ui-action-sequence"));
            checks.put("UI observations zone", hasClassElement(body, "details", "atf-ui-observations"));
            checks.put("inline screenshot", hasClassElement(body, "a", "atf-ui-action-screenshot-link"));
        } else {
            checks.put("API request zone", hasClassElement(body, "details", "request-evidence"));
            checks.put("API response zone", hasClassElement(body, "details", "response-evidence"));
            checks.put("API execution zone", hasClassElement(body, "details", "execution-evidence"));
        }
        return new Snapshot(report, checks, tokens, "");
    }

    private static String reportBody(String html) {
        int start = html.toLowerCase(java.util.Locale.ROOT).indexOf("<body");
        return start < 0 ? "" : html.substring(start);
    }

    private static boolean hasClassElement(String body, String element, String className) {
        String openingTag = "<" + element + "\\b[^>]*\\bclass\\s*=\\s*([\"'])(?:(?!\\1)[^>])*?"
                + "(?<![\\w-])" + Pattern.quote(className) + "(?![\\w-])(?:(?!\\1)[^>])*?\\1";
        return Pattern.compile(openingTag, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(body).find();
    }

    private static boolean has(Snapshot snapshot, String check) {
        return Boolean.TRUE.equals(snapshot.checks().get(check));
    }

    private static Optional<String> tokenValue(String source, String token) {
        int start = source.indexOf(token + ":");
        if (start < 0) return Optional.empty();
        int valueStart = start + token.length() + 1;
        int end = source.indexOf(';', valueStart);
        if (end < 0) return Optional.empty();
        String value = source.substring(valueStart, end).trim();
        return value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    public record Snapshot(Path report, Map<String, Boolean> checks,
                           Map<String, String> tokens, String readError) {
        public Snapshot(Path report, Map<String, Boolean> checks, String readError) {
            this(report, checks, Map.of(), readError);
        }

        public Snapshot {
            checks = Map.copyOf(checks);
            tokens = Map.copyOf(tokens);
            readError = readError == null ? "" : readError;
        }

        public static Snapshot failed(Path report, String error) {
            return new Snapshot(report, Map.of("readable HTML", false), Map.of(), error);
        }

        public boolean compliant() {
            return readError.isBlank() && checks.values().stream().allMatch(Boolean::booleanValue);
        }
    }

    public record Comparison(Snapshot ui, Snapshot api,
                             Map<String, Boolean> crossChecks) {
        public Comparison(Snapshot ui, Snapshot api) {
            this(ui, api, Map.of());
        }

        public Comparison {
            ui = Objects.requireNonNull(ui, "ui");
            api = Objects.requireNonNull(api, "api");
            crossChecks = Map.copyOf(Objects.requireNonNull(crossChecks, "crossChecks"));
        }

        public boolean compliant() {
            return ui.compliant() && api.compliant()
                    && crossChecks.values().stream().allMatch(Boolean::booleanValue);
        }
    }
}
