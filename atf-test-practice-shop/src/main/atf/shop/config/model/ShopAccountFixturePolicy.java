package atf.shop.config.model;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Stable ownership policy for the local authenticated test-account fixture.
 *
 * <p>The policy is deliberately separate from credential values. It can be
 * displayed in evidence without exposing a path, an email address or a
 * password. The ignored local fixture is reserved for a registered test
 * account; it is never a source of production credentials.</p>
 */
public enum ShopAccountFixturePolicy {
    SOURCE_TEST_RESOURCE(
            "registered local test-account fixture",
            "registered local account fixture; credentials redacted",
            "src/test/resources account fixture (credentials redacted)");

    private final String displayName;
    private final String expectedEvidence;
    private final String evidenceTarget;

    ShopAccountFixturePolicy(String displayName, String expectedEvidence,
                                     String evidenceTarget) {
        this.displayName = displayName;
        this.expectedEvidence = expectedEvidence;
        this.evidenceTarget = evidenceTarget;
    }

    public String displayName() {
        return displayName;
    }

    public String expectedEvidence() {
        return expectedEvidence;
    }

    public String evidenceTarget() {
        return evidenceTarget;
    }

    /**
     * Returns whether the path is the explicitly owned module test fixture.
     * A direct configuration constructor must obey the same boundary as the
     * YAML loader; the loader is not the only path through which configuration
     * can enter the module.
     */
    public boolean accepts(Path path) {
        Objects.requireNonNull(path, "path");
        Path normalized = path.toAbsolutePath().normalize();
        for (int index = 0; index + 2 < normalized.getNameCount(); index++) {
            if (normalized.getName(index).toString().equalsIgnoreCase("src")
                    && normalized.getName(index + 1).toString().equalsIgnoreCase("test")
                    && normalized.getName(index + 2).toString().equalsIgnoreCase("resources")) {
                return true;
            }
        }
        return false;
    }

    public void validate(Path path) {
        if (!accepts(path)) {
            throw new IllegalArgumentException("Practice Shop account files must use the "
                    + "module-owned src/test/resources fixture");
        }
    }

}
