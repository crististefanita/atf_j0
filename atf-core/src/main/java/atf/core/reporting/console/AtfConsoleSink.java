package atf.core.reporting.console;

/** Output port for concise, renderer-neutral ATF console messages. */
@FunctionalInterface
public interface AtfConsoleSink {
    void publish(AtfConsoleEvent event);

    static AtfConsoleSink noOp() {
        return event -> { };
    }
}
