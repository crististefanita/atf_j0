package atf.validation.contract;
import atf.core.http.response.ApiResponse;
import atf.validation.evaluation.ValidationTable;

@FunctionalInterface
public interface ResponseValidationRule {
    void evaluate(ApiResponse response, ValidationTable table);

    default String name() {
        return getClass().getSimpleName();
    }
}
