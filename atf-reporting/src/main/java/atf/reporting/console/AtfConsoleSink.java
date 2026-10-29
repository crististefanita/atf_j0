package atf.reporting.console;

/** Diagnostic output is best-effort and cannot change report lifecycle or verdict. */
@FunctionalInterface
public interface AtfConsoleSink {
    void publish(AtfConsoleEvent event);
    static AtfConsoleSink noOp() { return event -> { }; }
    static AtfConsoleSink bestEffort(AtfConsoleSink sink) {
        if (sink == null) return noOp();
        return event -> {
            try { sink.publish(event); }
            catch (RuntimeException ignored) { /* Diagnostic output does not own the business outcome. */ }
        };
    }
}
