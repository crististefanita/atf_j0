package atf.core.http.auth;

import java.net.http.HttpRequest;

public interface ApiAuthenticator {
    void apply(HttpRequest.Builder request, String profileName);
    String describeMode(String profileName);
    String describeTokenState(String profileName);
}
