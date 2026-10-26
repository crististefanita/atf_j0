package atf.console;
import atf.core.reporting.console.AtfConsoleEvent;


/** Maintains only the indentation state of one console event stream. */
final class ConsoleLifecycleState {
    private int depth;

    /**
     * Returns the indentation to use for the event and applies its nesting
     * transition. START is rendered before entering its child scope; COMPLETE
     * leaves the scope before it is rendered.
     */
    int depthFor(AtfConsoleEvent.Type type) {
        return switch (type) {
            case START -> {
                int current = depth;
                depth++;
                yield current;
            }
            case COMPLETE -> {
                depth = Math.max(0, depth - 1);
                yield depth;
            }
            case DETAIL, CODE, REPORT_PUBLISHED -> depth;
        };
    }

    int currentDepth() {
        return depth;
    }
}
