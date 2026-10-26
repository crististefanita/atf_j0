package atf.autoex.support.account;

import atf.autoex.config.AutoExConfiguration;

import java.util.UUID;

/** Synthetic per-run identity; never sourced from a personal credential store. */
public record AutoExTestAccount(String signupName, String email, String password,
                                AutoExConfiguration.Registration profile) {
    public static AutoExTestAccount create(AutoExConfiguration.Registration profile) {
        String unique = UUID.randomUUID().toString().substring(0, 10);
        return new AutoExTestAccount("ATF AutoEx " + unique,
                "atf.autoex." + unique + "@example.test", "ATF-AutoEx-" + unique + "!", profile);
    }
}
