package atf.shop.support.api;

import atf.core.http.response.ApiExecutionResult;

import java.time.Instant;
import java.util.Objects;

/** Adapts direct API executions to the same exchange contract used by UI evidence. */
public final class ShopApiExecutionExchange {
    private ShopApiExecutionExchange() { }

    public static ShopApiNetworkJournal.Exchange from(ApiExecutionResult execution,
                                                               long sequence,
                                                               String phase) {
        Objects.requireNonNull(execution, "execution");
        Objects.requireNonNull(execution.request(), "execution.request");
        Objects.requireNonNull(execution.response(), "execution.response");
        var request = execution.request();
        var response = execution.response();
        return new ShopApiNetworkJournal.Exchange(
                request.method(), request.path(), response.url(), response.status(),
                request.bodyText(), response.body(), request.headers(), response.headers(),
                sequence, Instant.now(), phase, "", "",
                "BusinessFlow", phase,
                atf.shop.api.ShopApiOperation.reportTitle(request.method(), request.path()),
                "HTTP_CLIENT", "", "status=" + response.status());
    }
}
