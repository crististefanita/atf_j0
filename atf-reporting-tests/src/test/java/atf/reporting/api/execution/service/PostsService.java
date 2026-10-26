package atf.reporting.api.execution.service;
import atf.core.http.execution.ApiExecutor;
import atf.core.http.response.ApiExecutionResult;
import atf.reporting.api.execution.routing.ApiRoutes;
import atf.core.json.JsonPayloads;
import atf.reporting.api.execution.model.PostRequest;

import java.util.Map;

public final class PostsService {
    private final ApiExecutor executor;

    public PostsService(ApiExecutor executor) {
        this.executor = executor;
    }

    public ApiExecutionResult getCollection() {
        return executor.execute(ApiRoutes.postsCollection().request(Map.of()));
    }

    public ApiExecutionResult get(int id) {
        return executor.execute(ApiRoutes.postItem().request(Map.of("id", id)));
    }

    public ApiExecutionResult forUser(int userId) {
        return executor.execute(ApiRoutes.userPosts().request(Map.of("userId", userId)));
    }

    public ApiExecutionResult create(PostRequest request) {
        return executor.execute(ApiRoutes.postCreate().requestBody(JsonPayloads.write(request), Map.of()));
    }
}
