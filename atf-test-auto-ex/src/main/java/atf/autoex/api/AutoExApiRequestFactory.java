package atf.autoex.api;

import atf.core.http.auth.AuthProfiles;
import atf.core.http.request.ApiRequest;
import atf.core.http.request.HttpHeader;
import atf.core.http.request.HttpMethod;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static atf.core.http.request.MediaTypes.APPLICATION_FORM_URLENCODED;
import static atf.autoex.api.AutoExApiFormField.EMAIL;
import static atf.autoex.api.AutoExApiFormField.PASSWORD;
import static atf.autoex.api.AutoExApiFormField.SEARCH_PRODUCT;

/** Pure form/query request construction for the consumer's documented API contract. */
public final class AutoExApiRequestFactory {
    public ApiRequest productsList() { return get(AutoExApiRoute.PRODUCTS_LIST); }
    public ApiRequest postProductsList() { return form(HttpMethod.POST, AutoExApiRoute.PRODUCTS_LIST, Map.of()); }
    public ApiRequest brandsList() { return get(AutoExApiRoute.BRANDS_LIST); }
    public ApiRequest putBrandsList() { return form(HttpMethod.PUT, AutoExApiRoute.BRANDS_LIST, Map.of()); }

    public ApiRequest searchProduct(String term) {
        return form(HttpMethod.POST, AutoExApiRoute.SEARCH_PRODUCT,
                term == null ? Map.of() : Map.of(SEARCH_PRODUCT.value(), term));
    }

    public ApiRequest verifyLogin(String email, String password) {
        return form(HttpMethod.POST, AutoExApiRoute.VERIFY_LOGIN,
                credentials(email, password));
    }

    public ApiRequest verifyLoginWithoutEmail(String password) {
        return form(HttpMethod.POST, AutoExApiRoute.VERIFY_LOGIN, credentials(null, password));
    }

    public ApiRequest deleteMethodOnVerifyLogin() {
        return form(HttpMethod.DELETE, AutoExApiRoute.VERIFY_LOGIN, Map.of());
    }

    public ApiRequest createAccount(AutoExApiAccount account) {
        return form(HttpMethod.POST, AutoExApiRoute.CREATE_ACCOUNT,
                Objects.requireNonNull(account, "account").formFields());
    }

    public ApiRequest deleteAccount(String email, String password) {
        return form(HttpMethod.DELETE, AutoExApiRoute.DELETE_ACCOUNT,
                credentials(email, password));
    }

    public ApiRequest updateAccount(AutoExApiAccount account) {
        return form(HttpMethod.PUT, AutoExApiRoute.UPDATE_ACCOUNT,
                Objects.requireNonNull(account, "account").formFields());
    }

    public ApiRequest userDetail(String email) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("email is required");
        return new ApiRequest(HttpRequest.BodyPublishers.noBody(), HttpMethod.GET.value(),
                AutoExApiRoute.USER_DETAIL_BY_EMAIL.path(), Map.of(EMAIL.value(), email.trim()), Map.of(),
                AuthProfiles.NONE, null, "");
    }

    private static ApiRequest get(AutoExApiRoute route) {
        return new ApiRequest(HttpRequest.BodyPublishers.noBody(), HttpMethod.GET.value(),
                route.path(), Map.of(), Map.of(), AuthProfiles.NONE, null, "");
    }

    private static ApiRequest form(HttpMethod method, AutoExApiRoute route, Map<String, String> fields) {
        Map<String, String> safeFields = fields == null ? Map.of() : fields;
        String body = safeFields.entrySet().stream()
                .map(field -> encode(field.getKey()) + "=" + encode(field.getValue()))
                .collect(java.util.stream.Collectors.joining("&"));
        Map<String, String> headers = !body.isEmpty()
                ? Map.of(HttpHeader.CONTENT_TYPE.value(), APPLICATION_FORM_URLENCODED) : Map.of();
        return new ApiRequest(body.isEmpty() ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body),
                method.value(), route.path(), Map.of(), headers, AuthProfiles.NONE, null, body);
    }

    private static Map<String, String> credentials(String email, String password) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (email != null) fields.put(EMAIL.value(), email);
        if (password != null) fields.put(PASSWORD.value(), password);
        return fields;
    }

    private static String encode(String value) {
        return URLEncoder.encode(Objects.requireNonNull(value, "form value"), StandardCharsets.UTF_8);
    }
}
