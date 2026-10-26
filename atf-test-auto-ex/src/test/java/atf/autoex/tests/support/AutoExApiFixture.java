package atf.autoex.tests.support;

import java.net.URI;

/** Reserved offline origin shared by deterministic API fixtures; never a live endpoint. */
public final class AutoExApiFixture {
    public static final URI BASE_URI = URI.create("https://automationexercise.invalid");

    private AutoExApiFixture() { }
}
