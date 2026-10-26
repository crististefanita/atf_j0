package atf.validation.ai;

import atf.validation.ai.grounding.AiGroundingObservation;
import atf.validation.ai.grounding.AiSourceReference;
import atf.validation.ai.invocation.AiInvocationEvidence;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Pure field comparator and weighted score calculator; it never invokes a model or renderer. */
public final class AiEvaluationEvaluator {

    private final AiEvaluationPolicy policy;
    private final Clock clock;

    public AiEvaluationEvaluator() {
        this(AiEvaluationPolicy.defaultPolicy(), Clock.systemUTC());
    }

    public AiEvaluationEvaluator(AiEvaluationPolicy policy) {
        this(policy, Clock.systemUTC());
    }

    public AiEvaluationEvaluator(AiEvaluationPolicy policy, Clock clock) {
        this.policy = Objects.requireNonNull(policy, "policy");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public AiEvaluationSnapshot evaluate(AiEvaluationCase evaluationCase, Map<String, ?> observedFields) {
        Objects.requireNonNull(observedFields, "observedFields");
        return evaluate(evaluationCase, observedFields,
                new AiInvocationEvidence(AiInvocationEvidence.Mode.UNKNOWN, null, null, null,
                        !observedFields.isEmpty(), null, null, List.of()));
    }

    public AiEvaluationSnapshot evaluate(AiEvaluationCase evaluationCase, Map<String, ?> observedFields,
                                         AiInvocationEvidence invocation) {
        return evaluate(evaluationCase, observedFields, invocation, Map.of());
    }

    public AiEvaluationSnapshot evaluate(AiEvaluationCase evaluationCase, Map<String, ?> observedFields,
                                         AiInvocationEvidence invocation,
                                         Map<String, AiSourceReference> groundingSources) {
        Objects.requireNonNull(evaluationCase, "evaluationCase");
        Objects.requireNonNull(observedFields, "observedFields");
        Objects.requireNonNull(invocation, "invocation");
        Objects.requireNonNull(groundingSources, "groundingSources");
        var comparisons = new ArrayList<AiFieldComparison>();
        var hardFailures = new ArrayList<AiEvaluationSnapshot.HardFailure>();
        long totalWeight = 0;
        long earnedWeight = 0;
        var outputReference = invocation.reference(AiInvocationEvidence.ReferenceKind.OUTPUT);
        var outputReferenceId = outputReference == null ? "" : outputReference.referenceId();
        var declaredPaths = new HashSet<String>();
        var groundingObservations = new ArrayList<AiGroundingObservation>();

        for (var expectation : evaluationCase.expectations()) {
            declaredPaths.add(expectation.fieldPath());
            var present = observedFields.containsKey(expectation.fieldPath());
            var observed = observedFields.get(expectation.fieldPath());
            var matched = matches(expectation, present, observed);
            totalWeight += expectation.weight();
            if (matched) earnedWeight += expectation.weight();
            var reason = reason(expectation, present, observed, matched);
            if (!matched && expectation.critical()) {
                hardFailures.add(new AiEvaluationSnapshot.HardFailure(hardFailRule(expectation, present, observed),
                        expectation.fieldPath(), expectation.comparison().expectedValue(), observed,
                        reason, outputReferenceId));
            }
            comparisons.add(new AiFieldComparison(expectation.fieldPath(), expectation.comparison().expectedValue(), observed,
                    expectation.comparison().mode(), matched ? AiFieldComparison.Outcome.MATCH : AiFieldComparison.Outcome.MISMATCH,
                    expectation.inputPresence(), expectation.critical(), expectation.weight(), reason, outputReferenceId));
            var grounding = grounding(expectation, groundingSources.get(expectation.fieldPath()));
            groundingObservations.add(grounding);
            if (grounding.result() != AiGroundingObservation.Result.SUPPORTED
                    && grounding.result() != AiGroundingObservation.Result.NOT_REQUIRED
                    && expectation.groundingRequirement().required()) {
                hardFailures.add(new AiEvaluationSnapshot.HardFailure(
                        grounding.result() == AiGroundingObservation.Result.UNSUPPORTED
                                ? "UNSUPPORTED_GROUNDING" : "MISSING_GROUNDING_REFERENCE",
                        expectation.fieldPath(), null, null, grounding.reasonCode(),
                        grounding.sourceReference() == null ? "" : grounding.sourceReference().sourceId()));
            }
        }
        for (var entry : observedFields.entrySet()) {
            if (declaredPaths.contains(entry.getKey())) continue;
            totalWeight++;
            var undeclaredIsFailure = policy.rejectUndeclaredOutputFields();
            if (undeclaredIsFailure) {
                hardFailures.add(new AiEvaluationSnapshot.HardFailure("UNDECLARED_OUTPUT_FIELD", entry.getKey(),
                        null, entry.getValue(), "UNDECLARED_OUTPUT_FIELD", outputReferenceId));
            }
            comparisons.add(new AiFieldComparison(entry.getKey(), null, entry.getValue(),
                    AiFieldExpectation.ComparisonMode.UNDECLARED, AiFieldComparison.Outcome.MISMATCH,
                    AiFieldExpectation.InputPresence.UNKNOWN, undeclaredIsFailure, 1,
                    "UNDECLARED_OUTPUT_FIELD", outputReferenceId));
        }

        int score = (int) Math.round(100.0 * earnedWeight / totalWeight);
        if (invocation.mode() == AiInvocationEvidence.Mode.REAL_AI && !invocation.completeRealInvocation()) {
            hardFailures.add(new AiEvaluationSnapshot.HardFailure("MISSING_INVOCATION_EVIDENCE", "", null, null,
                    "MISSING_INVOCATION_EVIDENCE", ""));
        }
        if (invocation.mode() == AiInvocationEvidence.Mode.UNKNOWN && invocation.outputProduced()) {
            hardFailures.add(new AiEvaluationSnapshot.HardFailure("UNCLASSIFIED_OUTPUT", "", null, null,
                    "UNCLASSIFIED_OUTPUT", ""));
        }

        boolean scoreEligible = invocation.completeRealInvocation();
        boolean evidenceFailure = hardFailures.stream().anyMatch(failure ->
                failure.ruleId().equals("MISSING_INVOCATION_EVIDENCE") || failure.ruleId().equals("UNCLASSIFIED_OUTPUT"));
        var verdict = evidenceFailure ? AiEvaluationSnapshot.Verdict.FAIL
                : !scoreEligible ? AiEvaluationSnapshot.Verdict.NOT_EVALUATED
                : !hardFailures.isEmpty() || score < policy.minimumPassingScore()
                ? AiEvaluationSnapshot.Verdict.FAIL : AiEvaluationSnapshot.Verdict.PASS;
        return AiEvaluationSnapshot.complete(evaluationCase.caseId(), evaluationCase.version(),
                evaluationCase.inputReference(), scoreEligible ? score : null, verdict,
                hardFailures, invocation, comparisons, groundingObservations, policy, clock.instant());
    }

    private static AiGroundingObservation grounding(AiFieldExpectation expectation, AiSourceReference source) {
        var requirement = expectation.groundingRequirement();
        if (!requirement.required()) {
            return new AiGroundingObservation(expectation.fieldPath(), source,
                    AiGroundingObservation.Result.NOT_REQUIRED, "NOT_REQUIRED");
        }
        if (source == null) {
            return new AiGroundingObservation(expectation.fieldPath(), null,
                    AiGroundingObservation.Result.MISSING_REFERENCE, "MISSING_REFERENCE");
        }
        if (!source.fieldPath().equals(expectation.fieldPath())
                || !requirement.allowedSourceIds().contains(source.sourceId())) {
            return new AiGroundingObservation(expectation.fieldPath(), source,
                    AiGroundingObservation.Result.UNSUPPORTED, "UNSUPPORTED_SOURCE");
        }
        return new AiGroundingObservation(expectation.fieldPath(), source,
                AiGroundingObservation.Result.SUPPORTED, "SUPPORTED");
    }

    private static boolean matches(AiFieldExpectation expectation, boolean present, Object observed) {
        var comparison = expectation.comparison();
        if (expectation.inputPresence() == AiFieldExpectation.InputPresence.ABSENT && present && observed != null) return false;
        if (expectation.inputPresence() == AiFieldExpectation.InputPresence.PRESENT
                && comparison.mode() == AiFieldExpectation.ComparisonMode.ABSENT) return false;
        return switch (comparison.mode()) {
            case EXACT -> present && comparison.expectedValue().equals(observed);
            case NORMALIZED_TEXT -> present && observed instanceof String text
                    && AiFieldExpectation.Comparison.normalizeText((String) comparison.expectedValue())
                    .equals(AiFieldExpectation.Comparison.normalizeText(text));
            case SET -> present && sameSet(comparison.expectedValue(), observed);
            case ORDERED_LIST -> present && comparison.expectedValue().equals(observed);
            case RANGE -> present && inRange(comparison, observed);
            case ALLOWED_VARIANTS -> present && ((List<?>) comparison.expectedValue()).contains(observed);
            case PRESENT -> present && observed != null && (!(observed instanceof String text) || !text.isBlank());
            case ABSENT -> !present || observed == null;
            case UNDECLARED -> false;
        };
    }

    private static boolean sameSet(Object expected, Object observed) {
        if (!(expected instanceof List<?> expectedList) || !(observed instanceof List<?> observedList)) return false;
        return expectedList.size() == observedList.size()
                && new HashSet<>(expectedList).equals(new HashSet<>(observedList));
    }

    private static boolean inRange(AiFieldExpectation.Comparison comparison, Object observed) {
        if (!(observed instanceof Number value)) return false;
        try {
            var decimal = new BigDecimal(value.toString());
            return decimal.compareTo(comparison.minimum()) >= 0 && decimal.compareTo(comparison.maximum()) <= 0;
        } catch (NumberFormatException invalidNumber) {
            return false;
        }
    }

    private static String reason(AiFieldExpectation expectation, boolean present, Object observed, boolean matched) {
        if (matched) return "MATCH";
        if (expectation.inputPresence() == AiFieldExpectation.InputPresence.PRESENT
                && expectation.comparison().mode() == AiFieldExpectation.ComparisonMode.ABSENT) {
            return "SOURCE_PRESENT_BUT_EXPECTED_ABSENT";
        }
        if (expectation.inputPresence() == AiFieldExpectation.InputPresence.ABSENT && present && observed != null) {
            return expectation.critical() ? "INVENTED_CRITICAL_ENTITY" : "UNEXPECTED_PRESENT";
        }
        if (!present || observed == null || (expectation.comparison().mode() == AiFieldExpectation.ComparisonMode.PRESENT
                && observed instanceof String text && text.isBlank())) return expectation.critical()
                && expectation.comparison().mode() != AiFieldExpectation.ComparisonMode.ABSENT
                ? "MISSING_CRITICAL_FIELD" : "MISSING_OBSERVED";
        if (expectation.comparison().mode() == AiFieldExpectation.ComparisonMode.ABSENT) return "UNEXPECTED_PRESENT";
        if (typeMismatch(expectation.comparison(), observed)) return "TYPE_MISMATCH";
        return "VALUE_MISMATCH";
    }

    private static boolean typeMismatch(AiFieldExpectation.Comparison comparison, Object observed) {
        return switch (comparison.mode()) {
            case NORMALIZED_TEXT -> !(observed instanceof String);
            case SET, ORDERED_LIST -> !(observed instanceof List<?>);
            case RANGE -> !(observed instanceof Number);
            case UNDECLARED -> true;
            default -> false;
        };
    }

    private static String hardFailRule(AiFieldExpectation expectation, boolean present, Object observed) {
        if (expectation.inputPresence() == AiFieldExpectation.InputPresence.PRESENT
                && expectation.comparison().mode() == AiFieldExpectation.ComparisonMode.ABSENT) {
            return "SOURCE_PRESENT_BUT_EXPECTED_ABSENT";
        }
        if (expectation.inputPresence() == AiFieldExpectation.InputPresence.ABSENT && present && observed != null) {
            return "INVENTED_CRITICAL_ENTITY";
        }
        if ((!present || observed == null || (expectation.comparison().mode() == AiFieldExpectation.ComparisonMode.PRESENT
                && observed instanceof String text && text.isBlank()))
                && expectation.comparison().mode() != AiFieldExpectation.ComparisonMode.ABSENT) {
            return "MISSING_CRITICAL_FIELD";
        }
        return "CRITICAL_FIELD_MISMATCH";
    }
}
