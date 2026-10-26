package atf.reporting.api.execution.service;
import atf.core.http.execution.ApiExecutor;
import atf.core.http.response.ApiExecutionResult;
import atf.reporting.api.execution.routing.ApiRoutes;
import atf.core.json.JsonPayloads;
import atf.reporting.api.execution.model.UserRequest;

import java.util.Map;

public final class UsersService {
    private final ApiExecutor executor;

    public UsersService(ApiExecutor executor) {
        this.executor = executor;
    }

    public ApiExecutionResult getCollection() {
        return executor.execute(ApiRoutes.usersCollection().request(Map.of()));
    }

    public ApiExecutionResult get(int id) {
        return executor.execute(ApiRoutes.userItem().request(Map.of("id", id)));
    }

    public ApiExecutionResult create(UserRequest request) {
        return executor.execute(ApiRoutes.userCreate().requestBody(JsonPayloads.write(request), Map.of()));
    }
}
