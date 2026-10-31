package atf.validation.contract;

/** Canonical API contracts kept independent from report rendering. */
public final class ContractValidators {
    private ContractValidators() { }

    public static ResponseContract userItem() {
        return new ResponseContract()
                .status(200)
                .contentTypeJson()
                .jsonPathExists("$.id")
                .jsonPathExists("$.name");
    }

    public static ResponseContract userCollection() {
        return new ResponseContract()
                .status(200)
                .contentTypeJson()
                .jsonPathExists("$[0].id")
                .jsonPathExists("$[0].name")
                .jsonPathExists("$[0].email");
    }

    public static ResponseContract postItem() {
        return new ResponseContract()
                .status(200)
                .contentTypeJson()
                .jsonPathExists("$.id")
                // The live API contract is snake_case; keep this aligned with
                // the .NET validator and the payload returned by GoREST.
                .jsonPathExists("$.user_id");
    }

    public static ResponseContract postCollection() {
        return new ResponseContract()
                .status(200)
                .contentTypeJson()
                .jsonPathExists("$[0].id")
                .jsonPathExists("$[0].user_id")
                .jsonPathExists("$[0].title")
                .jsonPathExists("$[0].body");
    }

    public static ResponseContract relationshipCollection(String userId) {
        return new ResponseContract()
                .status(200)
                .contentTypeJson()
                .jsonPathExists("$[0].id")
                .jsonPathExists("$[0].title")
                .everyArrayItemEquals("$", "user_id", userId);
    }
}
