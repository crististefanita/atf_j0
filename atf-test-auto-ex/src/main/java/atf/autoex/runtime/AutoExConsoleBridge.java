package atf.autoex.runtime;

import atf.core.reporting.console.AtfConsoleEvent;
import atf.core.reporting.console.AtfConsoleSink;
import java.util.Objects;

/** Consumer composition of the independent Reporting and Core console ports. */
final class AutoExConsoleBridge {
    private AutoExConsoleBridge() { }

    static atf.reporting.console.AtfConsoleSink reportingSink(AtfConsoleSink console) {
        Objects.requireNonNull(console, "console");
        return event -> console.publish(AtfConsoleEvent.fromExternalEnums(
                event.type(), event.scope(), event.name(), event.status(),
                event.durationMillis(), event.detailName(), event.detailValue()));
    }
}
