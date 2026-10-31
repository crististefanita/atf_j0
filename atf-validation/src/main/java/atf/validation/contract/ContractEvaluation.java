package atf.validation.contract;

import atf.validation.contract.json.JsonDocument;

import atf.core.http.response.ApiResponse;
import atf.validation.evaluation.ValidationTable;

/** Evaluation-local lazy parsing; no document state survives an evaluation. */
final class ContractEvaluation {
    private final ApiResponse response;
    private final ValidationTable table;
    private JsonDocument document;
    ContractEvaluation(ApiResponse response, ValidationTable table) { this.response = response; this.table = table; }
    ApiResponse response() { return response; }
    ValidationTable table() { return table; }
    JsonDocument document() {
        if (document == null) document = JsonDocument.parse(response.body());
        return document;
    }
    JsonPathSupport.Value at(String path) { return JsonPathSupport.read(document(), path); }
}
