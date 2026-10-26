package atf.core.reporting.inventory;
import atf.core.reporting.contract.AtfReportStatus;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Renderer-neutral identity of one scenario in the current run.
 *
 * <p>Overview, Category and Fails must consume the same population. Keeping
 * this value object in core prevents each renderer from inventing its own
 * counters or interpreting Extent's container nodes as tests.</p>
 */
public record AtfTestCaseInventory(
        String id,
        String testClass,
        String scenario,
        AtfReportStatus status,
        Collection<String> tags,
        Duration duration,
        String failurePhase,
        String failureMessage) {

    public AtfTestCaseInventory {
        id = requireText(id, "id");
        testClass = requireText(testClass, "testClass");
        scenario = requireText(scenario, "scenario");
        status = Objects.requireNonNull(status, "status");
        tags = tags == null ? List.of() : tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
        duration = duration == null ? Duration.ZERO : duration;
        failurePhase = failurePhase == null ? "" : failurePhase.trim();
        failureMessage = failureMessage == null ? "" : failureMessage.trim();
    }

    public boolean failed() {
        return status == AtfReportStatus.FAIL;
    }

    public boolean warning() {
        return status == AtfReportStatus.WARNING;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
