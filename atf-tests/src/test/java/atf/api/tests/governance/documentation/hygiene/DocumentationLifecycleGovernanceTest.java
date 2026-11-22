package atf.api.tests.governance.documentation.hygiene;

import atf.api.tests.governance.documentation.DocumentationGovernanceSupport;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static atf.api.tests.governance.AtfGovernanceTestSupport.locateProjectRoot;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps execution receipts and completed backlog items out of maintained contracts. */
@AtfComponentTest
public class DocumentationLifecycleGovernanceTest {
    private static final Pattern FENCE = Pattern.compile("^\\s*(`{3,}|~{3,})(.*)$");
    private static final Pattern CONSUMER = Pattern.compile("(?i)\\b(?:J\\d+|atf[_-]j\\d+|consumer|consumator)\\b");
    private static final Pattern RESULT = Pattern.compile("(?iu)\\b(?:PASS(?:ED)?|FAIL(?:ED)?|verified|verificat(?:e|ă|i)?|livrat(?:e|ă|i)?|delivered)\\b");
    private static final Pattern ACCEPTED = Pattern.compile("(?iu)\\b(?:PASS(?:ED)?|verified|verificat(?:e|ă|i)?|livrat(?:e|ă|i)?|delivered)\\b");
    private static final Pattern RUN_FACT = Pattern.compile(
            "(?iu)\\b(?:20\\d{2}[-/]\\d{2}[-/]\\d{2}|\\d+\\s*/\\s*\\d+\\s+(?:tests?|checks?|cases?|verificări|teste|cazuri|probe))\\b");
    private static final Pattern RECEIPT_TITLE = Pattern.compile(
            "(?i)(?:consumer verification|verificare consumer|dovezi(?:le)? consumer|consumer evidence|provenance|proveniență)");
    private static final Pattern HISTORICAL_RECEIPT = Pattern.compile(
            "(?i)historical evidence not locally available|dovezi istorice indisponibile local");
    private static final Pattern CLOSED_STATUS = Pattern.compile(
            "(?iu)\\b(?:status|stare)\\s*:\\s*(?:done|closed|resolved|completed|verified|rezolvat|închis|încheiat|verificat)\\b");
    private static final Pattern COMPLETED_TASK = Pattern.compile("(?i)^\\s*[-+*]\\s+\\[x]\\s+.*");
    private static final String BASELINE = "docs/ATF_Test_Changes_Baseline.md";

    @Test
    void maintainedDocumentsKeepContractsAndActiveGapsInsteadOfDeliveryReceipts() throws Exception {
        Path root = locateProjectRoot();
        var violations = new ArrayList<DocumentationGovernanceSupport.Violation>();
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(path -> DocumentationGovernanceSupport.isGovernedMarkdown(root, path)).toList()) {
                violations.addAll(inspect(root, file));
            }
        }
        DocumentationGovernanceSupport.assertNoViolations("Documentation lifecycle governance", violations);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "# Core C3 — J1 Consumer Verification 2026-11-02",
            "## J4 consumer evidence — delivered",
            "## J1 — dovezi consumer livrate",
            "J1 — 24/24 verificări trecute, livrare verificată.",
            "J1 consumer verification passed\n24/24 checks on the delivered pair.",
            "| atf_j1 | 2026-11-22 | PASS |",
            "<summary>J21 provenance — historical evidence not locally available</summary>"
    })
    void rejectsConsumerReceiptsIncludingWrappedProse(String prose, @TempDir Path root) throws Exception {
        Path file = write(root, "docs/contract.md", prose);
        var violations = inspect(root, file);
        assertTrue(violations.stream().anyMatch(item -> item.code().equals("DOC-HISTORY-CONSUMER")
                && item.line() == 1), () -> "receipt must identify its source line: " + violations);
    }

    @ParameterizedTest
    @ValueSource(strings = {"**Status: Resolved.** The contract is published.",
            "**Stare: Închis.** Contractul aparține modulului.", "- [x] Publish the consumer evidence."})
    void rejectsCompletedBacklogItems(String prose, @TempDir Path root) throws Exception {
        var violations = inspect(root, write(root, "docs/feedback/owner.md", prose));
        assertTrue(violations.stream().anyMatch(item -> item.code().equals("DOC-FEEDBACK-CLOSED")));
    }

    @Test
    void retainsContractsOpenFindingsLiteralExamplesAndExecutableChangeControls(@TempDir Path root) throws Exception {
        String contract = "# Independent consumer verification\n\n"
                + "J1 verifies the physical JAR independently; PASS requires actual execution.\n\n"
                + "~~~text\nJ1 consumer evidence delivered 24/24\n~~~\n"
                + "````markdown\n```\nJ1 passed 24/24\n````\n";
        assertTrue(inspect(root, write(root, "README.md", contract)).isEmpty());
        String open = "# Active criterion\n\n**Status: Open.** J4 fails on the final row.\n\n"
                + "- [ ] Preserve the exact owner and last row.\n\n```yaml\nstatus: VERIFIED\n```\n";
        assertTrue(inspect(root, write(root, "docs/feedback/owner.md", open)).isEmpty());
        assertTrue(inspect(root, write(root, "docs/feedback/incident.md",
                "# Active failure\n\nJ4 FAIL on 2026-11-22: the last comparison is hidden.\n")).isEmpty(),
                "an unresolved consumer failure may retain investigation evidence");
        assertTrue(inspect(root, write(root, "docs/feedback/priority.md",
                "**Priority: 88/100.** J4 needs an independent oracle for the delivered contract.\n")).isEmpty(),
                "a priority score is not an executed test count");
        assertTrue(inspect(root, write(root, BASELINE, "status: VERIFIED\nJ1 passed 24/24")).isEmpty(),
                "the executable change-control registry retains its historical bases");
    }

    private static Path write(Path root, String relative, String prose) throws Exception {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, prose);
        return file;
    }

    private static List<DocumentationGovernanceSupport.Violation> inspect(Path root, Path file) throws Exception {
        String relative = root.relativize(file).toString().replace('\\', '/');
        if (relative.equals(BASELINE)) return List.of();
        boolean feedback = relative.startsWith("docs/feedback/");
        var violations = new ArrayList<DocumentationGovernanceSupport.Violation>();
        List<String> lines = Files.readAllLines(file);
        String marker = null;
        var paragraph = new StringBuilder();
        int start = 1;
        for (int index = 0; index <= lines.size(); index++) {
            String line = index == lines.size() ? "" : lines.get(index);
            var fence = FENCE.matcher(line);
            boolean delimiter = fence.matches();
            if (marker != null) {
                if (delimiter && fence.group(1).charAt(0) == marker.charAt(0)
                        && fence.group(1).length() >= marker.length() && fence.group(2).isBlank()) marker = null;
                continue;
            }
            boolean structural = line.matches("^\\s*(?:#{1,6}\\s|\\||<|[-+*]\\s|\\d+[.)]\\s).*");
            if (line.isBlank() || delimiter || structural) {
                check(paragraph.toString(), start, feedback, root, file, violations);
                paragraph.setLength(0);
                if (delimiter) marker = fence.group(1);
                else if (structural) check(line, index + 1, feedback, root, file, violations);
            } else {
                if (paragraph.isEmpty()) start = index + 1;
                else paragraph.append(' ');
                paragraph.append(line);
            }
        }
        return List.copyOf(violations);
    }

    private static void check(String text, int line, boolean feedback, Path root, Path file,
                              List<DocumentationGovernanceSupport.Violation> violations) {
        String prose = text.replace("**", "");
        boolean title = prose.stripLeading().startsWith("#") || prose.contains("<summary>");
        boolean receipt = HISTORICAL_RECEIPT.matcher(prose).find()
                || CONSUMER.matcher(prose).find() && (RESULT.matcher(prose).find() && RUN_FACT.matcher(prose).find()
                && (!feedback || ACCEPTED.matcher(prose).find())
                || title && RECEIPT_TITLE.matcher(prose).find() && (RUN_FACT.matcher(prose).find() || RESULT.matcher(prose).find()));
        if (receipt) violations.add(DocumentationGovernanceSupport.violation("DOC-HISTORY-CONSUMER", root, file, line,
                "consumer delivery receipt in maintained documentation",
                "contracts describe reusable behavior; run receipts remain beside the owned package",
                "keep only the stable owner contract or an unresolved acceptance criterion; remove delivery history"));
        if (feedback && (CLOSED_STATUS.matcher(prose).find() || COMPLETED_TASK.matcher(prose).matches())) {
            violations.add(DocumentationGovernanceSupport.violation("DOC-FEEDBACK-CLOSED", root, file, line,
                    "completed item retained in the active backlog",
                    "feedback contains unresolved criteria, not a completed-task journal",
                    "promote useful rules to their owner, then remove the completed item; preserve active change controls"));
        }
    }
}
