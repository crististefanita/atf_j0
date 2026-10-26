package atf.console;
import atf.core.reporting.contract.AtfReportStatus;


import java.io.PrintStream;

/** Output capability policy for a concrete console stream. */
final class ConsoleOutputStyle {
    private final boolean unicodeSupported;

    private ConsoleOutputStyle(boolean unicodeSupported) {
        this.unicodeSupported = unicodeSupported;
    }

    static ConsoleOutputStyle forStream(PrintStream output) {
        boolean supportsUnicode = output != System.out
                && output.charset().newEncoder().canEncode("▶ • ✅ ⚠ ❌ ⏭ ℹ");
        return new ConsoleOutputStyle(supportsUnicode);
    }

    String startMarker() {
        return unicodeSupported ? "▶ " : "> ";
    }

    String detailMarker() {
        return unicodeSupported ? "• " : "- ";
    }

    String statusIcon(AtfReportStatus status) {
        if (!unicodeSupported) return "";
        return switch (status) {
            case PASS -> "✅";
            case WARNING -> "⚠";
            case FAIL -> "❌";
            case SKIP -> "⏭";
            case INFO -> "ℹ";
        };
    }
}
