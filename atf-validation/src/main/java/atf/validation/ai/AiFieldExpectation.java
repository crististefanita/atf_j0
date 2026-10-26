package atf.validation.ai;

import atf.validation.ai.grounding.AiGroundingRequirement;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** A versioned case's expected value for one stable semantic field path. */
public record AiFieldExpectation(
        String fieldPath,
        Comparison comparison,
        boolean critical,
        int weight,
        InputPresence inputPresence,
        AiGroundingRequirement groundingRequirement) {

    public AiFieldExpectation {
        fieldPath = requireText(fieldPath, "fieldPath");
        Objects.requireNonNull(comparison, "comparison");
        Objects.requireNonNull(inputPresence, "inputPresence");
        Objects.requireNonNull(groundingRequirement, "groundingRequirement");
        if (weight <= 0) throw new IllegalArgumentException("weight must be positive");
    }

    public AiFieldExpectation(String fieldPath, Comparison comparison, boolean critical, int weight,
                              InputPresence inputPresence) {
        this(fieldPath, comparison, critical, weight, inputPresence, AiGroundingRequirement.none());
    }

    public AiFieldExpectation(String fieldPath, ComparisonMode mode, Object expectedValue,
                              boolean critical, int weight) {
        this(fieldPath, Comparison.of(mode, expectedValue), critical, weight, InputPresence.UNKNOWN);
    }

    public AiFieldExpectation(String fieldPath, ComparisonMode mode, Object expectedValue,
                              boolean critical, int weight, InputPresence inputPresence) {
        this(fieldPath, Comparison.of(mode, expectedValue), critical, weight, inputPresence);
    }

    public AiFieldExpectation(String fieldPath, ComparisonMode mode, Object expectedValue,
                              boolean critical, int weight, InputPresence inputPresence,
                              AiGroundingRequirement groundingRequirement) {
        this(fieldPath, Comparison.of(mode, expectedValue), critical, weight, inputPresence, groundingRequirement);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }

    public enum InputPresence { PRESENT, ABSENT, UNKNOWN }

    public enum ComparisonMode { EXACT, NORMALIZED_TEXT, SET, ORDERED_LIST, RANGE, ALLOWED_VARIANTS, PRESENT, ABSENT, UNDECLARED }

    public record Comparison(ComparisonMode mode, Object expectedValue, BigDecimal minimum, BigDecimal maximum) {
        public Comparison {
            Objects.requireNonNull(mode, "mode");
            if (mode == ComparisonMode.RANGE) {
                Objects.requireNonNull(minimum, "minimum");
                Objects.requireNonNull(maximum, "maximum");
                if (minimum.compareTo(maximum) > 0) throw new IllegalArgumentException("minimum must not exceed maximum");
                expectedValue = null;
            } else if (mode == ComparisonMode.PRESENT || mode == ComparisonMode.ABSENT) {
                if (expectedValue != null) throw new IllegalArgumentException(mode + " does not accept an expected value");
                minimum = null;
                maximum = null;
            } else {
                expectedValue = AiEvaluationValues.immutable(Objects.requireNonNull(expectedValue, "expectedValue"));
                minimum = null;
                maximum = null;
                if (mode == ComparisonMode.NORMALIZED_TEXT && !(expectedValue instanceof String)) {
                    throw new IllegalArgumentException("NORMALIZED_TEXT requires a string expected value");
                }
                if ((mode == ComparisonMode.SET || mode == ComparisonMode.ORDERED_LIST
                        || mode == ComparisonMode.ALLOWED_VARIANTS) && !(expectedValue instanceof List<?>)) {
                    throw new IllegalArgumentException(mode + " requires a list expected value");
                }
                if (mode == ComparisonMode.SET && new HashSet<>((List<?>) expectedValue).size() != ((List<?>) expectedValue).size()) {
                    throw new IllegalArgumentException("SET expected values must be unique");
                }
                if (mode == ComparisonMode.ALLOWED_VARIANTS && ((List<?>) expectedValue).isEmpty()) {
                    throw new IllegalArgumentException("ALLOWED_VARIANTS must declare at least one value");
                }
            }
        }

        public static Comparison of(ComparisonMode mode, Object expected) {
            return new Comparison(mode, expected, null, null);
        }

        public static Comparison range(Number minimum, Number maximum) {
            return new Comparison(ComparisonMode.RANGE, null, decimal(minimum), decimal(maximum));
        }

        private static BigDecimal decimal(Number value) {
            Objects.requireNonNull(value, "range bound");
            try {
                return new BigDecimal(value.toString());
            } catch (NumberFormatException invalidNumber) {
                throw new IllegalArgumentException("range bounds must be finite numbers", invalidNumber);
            }
        }

        public static String normalizeText(String text) {
            return Normalizer.normalize(text, Normalizer.Form.NFKC).strip().replaceAll("\\s+", " ");
        }
    }
}
