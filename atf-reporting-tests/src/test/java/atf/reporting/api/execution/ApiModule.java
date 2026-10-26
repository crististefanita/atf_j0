package atf.reporting.api.execution;
import atf.core.config.ApiSettingsSnapshot;
import atf.core.http.execution.ApiExecutor;
import atf.reporting.api.execution.service.PostsService;
import atf.reporting.api.execution.service.UsersService;

import java.util.Objects;

/**
 * Scoped composition root for one API execution context.
 *
 * <p>The module owns one immutable settings snapshot and the services built
 * from that snapshot. It deliberately knows nothing about JUnit or Extent;
 * callers can use the same graph in live tests, offline tests or another
 * renderer.</p>
 */
public final class ApiModule {
    private final ApiSettingsSnapshot settings;
    private final ApiExecutor executor;
    private final UsersService users;
    private final PostsService posts;

    ApiModule(ApiSettingsSnapshot settings, ApiExecutor executor) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.users = new UsersService(executor);
        this.posts = new PostsService(executor);
    }

    public ApiSettingsSnapshot settings() { return settings; }
    public ApiExecutor executor() { return executor; }
    public UsersService users() { return users; }
    public PostsService posts() { return posts; }
}
