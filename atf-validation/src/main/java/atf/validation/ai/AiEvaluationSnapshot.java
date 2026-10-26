package atf.validation.ai;

import atf.validation.ai.grounding.AiGroundingObservation;
import atf.validation.ai.invocation.AiInvocationEvidence;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Completed, immutable semantic snapshot; no mutable or partially evaluated instance is exposed. */
public final class AiEvaluationSnapshot {

    private final String caseId;
    private final int caseVersion;
    private final String inputReference;
    private final Integer qualityScore;
    private final Verdict verdict;
    private final List<HardFailure> hardFailures;
    private final AiInvocationEvidence invocationEvidence;
    private final List<AiFieldComparison> comparisons;
    private final List<AiGroundingObservation> groundingObservations;
    private final AiEvaluationPolicy policy;
    private final Instant completedAt;

    private AiEvaluationSnapshot(String caseId, int caseVersion, String inputReference, Integer qualityScore,
                                 Verdict verdict, List<HardFailure> hardFailures,
                                 AiInvocationEvidence invocationEvidence, List<AiFieldComparison> comparisons,
                                 List<AiGroundingObservation> groundingObservations,
                                 AiEvaluationPolicy policy, Instant completedAt) {
        this.caseId = Objects.requireNonNull(caseId, "caseId");
        this.caseVersion = caseVersion;
        this.inputReference = Objects.requireNonNull(inputReference, "inputReference");
        this.qualityScore = qualityScore;
        this.verdict = Objects.requireNonNull(verdict, "verdict");
        this.hardFailures = List.copyOf(hardFailures);
        this.invocationEvidence = Objects.requireNonNull(invocationEvidence, "invocationEvidence");
        this.comparisons = List.copyOf(comparisons);
        this.groundingObservations = List.copyOf(groundingObservations);
        this.policy = Objects.requireNonNull(policy, "policy");
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt");
        validate();
    }

    static AiEvaluationSnapshot complete(String caseId, int caseVersion, String inputReference, Integer qualityScore,
                                         Verdict verdict, List<HardFailure> hardFailures,
                                         AiInvocationEvidence invocationEvidence, List<AiFieldComparison> comparisons,
                                         List<AiGroundingObservation> groundingObservations,
                                         AiEvaluationPolicy policy, Instant completedAt) {
        return new AiEvaluationSnapshot(caseId, caseVersion, inputReference, qualityScore, verdict,
                hardFailures, invocationEvidence, comparisons, groundingObservations, policy, completedAt);
    }

    private void validate() {
        if (caseId.isBlank() || caseVersion <= 0 || inputReference.isBlank() || comparisons.isEmpty()) {
            throw new IllegalArgumentException("completed snapshot requires case identity and comparisons");
        }
        if (qualityScore != null && (qualityScore < 0 || qualityScore > 100
                || !invocationEvidence.completeRealInvocation())) {
            throw new IllegalArgumentException("quality score requires complete REAL_AI invocation evidence");
        }
        if (verdict == Verdict.PASS && (qualityScore == null || !hardFailures.isEmpty())) {
            throw new IllegalArgumentException("PASS requires an eligible score and no hard failures");
        }
        if (verdict == Verdict.NOT_EVALUATED && qualityScore != null) {
            throw new IllegalArgumentException("NOT_EVALUATED cannot carry an AI quality score");
        }
    }

    public String caseId() { return caseId; }
    public int caseVersion() { return caseVersion; }
    public String inputReference() { return inputReference; }
    public Integer qualityScore() { return qualityScore; }
    public Verdict verdict() { return verdict; }
    public List<HardFailure> hardFailures() { return hardFailures; }
    public AiInvocationEvidence invocationEvidence() { return invocationEvidence; }
    public List<AiFieldComparison> comparisons() { return comparisons; }
    public List<AiGroundingObservation> groundingObservations() { return groundingObservations; }
    public AiEvaluationPolicy policy() { return policy; }
    public Instant completedAt() { return completedAt; }
    public Lifecycle lifecycle() { return Lifecycle.COMPLETE; }
    public boolean hardFail() { return !hardFailures.isEmpty(); }

    public enum Lifecycle { COMPLETE }
    public enum Verdict { PASS, FAIL, NOT_EVALUATED }

    public record HardFailure(String ruleId, String fieldPath, Object expected,
                              Object observed, String reasonCode, String evidenceReference) {
        public HardFailure {
            expected = AiEvaluationValues.immutable(expected);
            observed = AiEvaluationValues.immutable(observed);
        }
    }
}
