package atf.console.junit;

import atf.core.reporting.console.AtfConsoleEvent;
import atf.core.reporting.console.AtfConsoleSink;
import atf.testkit.reporting.AtfReportPath;
import atf.testkit.reporting.AtfReportPathPolicyResolver;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Aggregates report-path opt-in across the owners of one shared report artifact. */
public final class AtfReportPathOptIn implements AtfConsoleSink {
    private final AtomicBoolean enabled = new AtomicBoolean();
    private final AtfConsoleSink destination;

    public AtfReportPathOptIn(AtfConsoleSink destination) {
        this.destination = Objects.requireNonNull(destination, "destination");
    }

    /** Records one report owner; a single enabled owner opts the shared artifact in. */
    public void include(Class<?> owner) {
        if (AtfReportPathPolicyResolver.resolve(owner) == AtfReportPath.Policy.ENABLED) {
            enabled.set(true);
        }
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    @Override
    public void publish(AtfConsoleEvent event) {
        Objects.requireNonNull(event, "event");
        if (event.type() == AtfConsoleEvent.Type.REPORT_PUBLISHED && enabled.get()) {
            destination.publish(event);
        }
    }
}
