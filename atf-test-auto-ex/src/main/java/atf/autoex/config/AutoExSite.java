package atf.autoex.config;

import java.net.URI;

/** Public site identity; runtime endpoints always come from the selected configuration. */
public final class AutoExSite {
    public static final URI PUBLIC_BASE_URI = URI.create("https://automationexercise.com");

    private AutoExSite() { }
}
