package atf.shop.support.api;

import atf.shop.api.ShopApiOperation;

import java.util.List;
import java.util.function.Predicate;

/** Pure selection policy for the ordered API exchanges captured by a UI session. */
public final class ShopApiExchangeSelector {
    private ShopApiExchangeSelector() { }

    public static ShopApiNetworkJournal.Exchange latest(
            List<ShopApiNetworkJournal.Exchange> exchanges,
            String method, String path, Predicate<ShopApiNetworkJournal.Exchange> extra) {
        ShopApiNetworkJournal.Exchange exchange = latestOptional(exchanges, method, path, extra);
        if (exchange != null) return exchange;
        throw new IllegalStateException("Missing API exchange: " + method + " " + path
                + "; captured=" + exchanges.stream()
                .map(ShopApiNetworkJournal.Exchange::summary).toList());
    }

    public static ShopApiNetworkJournal.Exchange latest(
            List<ShopApiNetworkJournal.Exchange> exchanges,
            ShopApiOperation operation,
            Predicate<ShopApiNetworkJournal.Exchange> extra) {
        ShopApiNetworkJournal.Exchange exchange = latestOptional(exchanges, operation, extra);
        if (exchange != null) return exchange;
        throw new IllegalStateException("Missing API operation: " + operation.name()
                + "; captured=" + exchanges.stream()
                .map(ShopApiNetworkJournal.Exchange::summary).toList());
    }

    public static ShopApiNetworkJournal.Exchange latestOptional(
            List<ShopApiNetworkJournal.Exchange> exchanges,
            String method, String path, Predicate<ShopApiNetworkJournal.Exchange> extra) {
        for (int index = exchanges.size() - 1; index >= 0; index--) {
            ShopApiNetworkJournal.Exchange exchange = exchanges.get(index);
            if (method.equalsIgnoreCase(exchange.method())
                    && matchesRoute(exchange.path(), path) && extra.test(exchange)) {
                return exchange;
            }
        }
        return null;
    }

    public static ShopApiNetworkJournal.Exchange latestOptional(
            List<ShopApiNetworkJournal.Exchange> exchanges,
            ShopApiOperation operation,
            Predicate<ShopApiNetworkJournal.Exchange> extra) {
        for (int index = exchanges.size() - 1; index >= 0; index--) {
            ShopApiNetworkJournal.Exchange exchange = exchanges.get(index);
            if (operation.matches(exchange.method(), exchange.path()) && extra.test(exchange)) {
                return exchange;
            }
        }
        return null;
    }

    private static boolean matchesRoute(String actual, String expected) {
        String normalizedActual = normalize(actual);
        String normalizedExpected = normalize(expected);
        if (normalizedExpected.endsWith("/")) return normalizedActual.startsWith(normalizedExpected);
        return normalizedExpected.equals(normalizedActual);
    }

    static String normalize(String path) {
        if (path == null) return "";
        int query = path.indexOf('?');
        int fragment = path.indexOf('#');
        int end = query < 0 ? path.length() : query;
        if (fragment >= 0) end = Math.min(end, fragment);
        return path.substring(0, end);
    }

    public static String productIdFromPath(String selectedProductPath) {
        if (selectedProductPath == null || selectedProductPath.isBlank()) return null;
        String normalized = selectedProductPath.trim();
        int separator = normalized.lastIndexOf('/');
        if (separator < 0 || separator == normalized.length() - 1) return null;
        String id = normalized.substring(separator + 1).split("[?#]", 2)[0].trim();
        return id.isBlank() ? null : id;
    }
}
