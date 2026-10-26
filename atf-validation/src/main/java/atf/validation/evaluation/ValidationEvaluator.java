package atf.validation.evaluation;
import atf.core.http.response.ApiResponse;
import atf.validation.contract.ResponseContract;

    /** Connects the contract to the aggregate table and Pass/Warning/Fail policy. */
public final class ValidationEvaluator {
    private final int maxDisplayedCharacters;

    public ValidationEvaluator() {
        this(ValidationTable.DEFAULT_MAX_DISPLAYED_CHARACTERS);
    }

    public ValidationEvaluator(int maxDisplayedCharacters) {
        if (maxDisplayedCharacters <= 0) throw new IllegalArgumentException("display limit must be positive");
        this.maxDisplayedCharacters = maxDisplayedCharacters;
    }

    public ValidationDecision evaluate(ResponseContract contract, ApiResponse response, boolean warningMode) {
        return evaluate(contract, response,
                warningMode ? ValidationOutcomePolicy.warning() : ValidationOutcomePolicy.strict());
    }

    public ValidationDecision evaluate(ResponseContract contract, ApiResponse response,
                                       ValidationOutcomePolicy outcomePolicy) {
        if (contract == null || response == null) throw new NullPointerException("contract/response");
        if (outcomePolicy == null) throw new NullPointerException("outcomePolicy");
        var table = new ValidationTable(maxDisplayedCharacters);
        contract.evaluate(response, table);
        return outcomePolicy.decide(table);
    }
}
