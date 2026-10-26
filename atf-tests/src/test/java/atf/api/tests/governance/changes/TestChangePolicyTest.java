package atf.api.tests.governance.changes;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
public class TestChangePolicyTest {
    private static final String BASE = "a".repeat(40);
    private static final String PATH = "atf-core/src/test/java/a/ContractTest.java";
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);

    @Test
    void rejectsAnEditWithoutChangeControlAndExplainsTheExactRemedy() {
        var failure = check("M", List.of());
        assertEquals(1, failure.size());
        assertTrue(failure.getFirst().contains(PATH));
        assertTrue(failure.getFirst().contains(BASE));
        assertTrue(failure.getFirst().contains("relevant owner document in docs/feedback/**/*.md"));
        assertTrue(failure.getFirst().contains("ATF_Test_Changes_Baseline.md"));
    }

    @Test
    void acceptsExactActiveAndVerifiedRequests() {
        for (String status : List.of("ACTIVE", "VERIFIED")) {
            assertTrue(check("M", List.of(request(status, BASE, TODAY, false))).isEmpty());
        }
    }

    @Test
    void rejectsClosedDraftExpiredAndForeignBaselineRequests() {
        for (String status : List.of("CLOSED", "DRAFT")) {
            assertFalse(check("M", List.of(request(status, BASE, TODAY, false))).isEmpty());
        }
        assertFalse(check("M", List.of(request("ACTIVE", BASE, TODAY.minusDays(1), false))).isEmpty());
        assertFalse(check("M", List.of(request("ACTIVE", "b".repeat(40), TODAY, false))).isEmpty());
    }

    @Test
    void deletionIsExplicitAndConflictsNeverPass() {
        assertFalse(check("D", List.of(request("ACTIVE", BASE, TODAY, false))).isEmpty());
        assertTrue(check("D", List.of(request("ACTIVE", BASE, TODAY, true))).isEmpty());
        assertFalse(check("U", List.of(request("ACTIVE", BASE, TODAY, true))).isEmpty());
    }

    @Test
    void rejectsAmbiguousOwnershipAndDoesNotAuthorizeAnotherFile() {
        var first = request("ACTIVE", BASE, TODAY, false);
        var second = new TestChangeRequest("OTHER-01", first.status(), first.base(), first.expires(),
                first.reason(), first.proof(), first.paths(), false, "other.md");
        assertTrue(check("M", List.of(first, second)).getFirst().contains("AMBIGUOUS"));
        assertTrue(check("M", List.of(request("CLOSED", BASE, TODAY, false), second)).isEmpty(),
                "a closed control transfers ownership to exactly one active request");
        assertFalse(TestChangePolicy.validate(List.of(new GitTestChanges.Change("M", PATH + ".other")),
                List.of(first), BASE, TODAY).isEmpty());
    }

    @Test
    void buildConfigurationCannotBypassTestGovernance() throws Exception {
        for (String path : List.of("pom.xml", "atf-core/pom.xml", "new-module/pom.xml")) {
            var change = new GitTestChanges.Change("M", path);
            assertTrue(TestChangePolicy.protectedPath(path), path);
            assertEquals(Set.of(path), FeedbackTestChangeRequests.parse(markdown(path), "build.md")
                    .getFirst().paths());
            assertFalse(TestChangePolicy.validate(List.of(change), List.of(), BASE, TODAY).isEmpty(),
                "test execution settings require an explicit change control: " + path);
            var approval = new TestChangeRequest("BUILD-01", "ACTIVE", BASE, TODAY,
                    "Review changes to the test execution configuration", "mvn test",
                    Set.of(path), false, "build.md");
            assertTrue(TestChangePolicy.validate(List.of(change), List.of(approval), BASE, TODAY).isEmpty());
        }
        assertFalse(TestChangePolicy.isExactPath("README.md"));
        assertFalse(TestChangePolicy.protectedPath("atf-test-practice-shop/pom.xml"));
    }

    @Test
    void protectsHelpersResourcesAndNewModulesButExemptsOnlyTheShop() {
        for (String path : List.of(PATH, "atf-reporting-tests/src/test/resources/junit-platform.properties",
                "atf-tests/src/test/Support.java", "new-module/src/test/java/NewTest.java", "atf-tests/pom.xml")) {
            assertTrue(TestChangePolicy.protectedPath(path), path);
        }
        assertFalse(TestChangePolicy.protectedPath("atf-test-practice-shop/src/test/ShopTest.java"));
        assertTrue(TestChangePolicy.validate(List.of(new GitTestChanges.Change("M",
                "atf-test-practice-shop/src/test/ShopTest.java")), List.of(), BASE, TODAY).isEmpty());
        assertFalse(TestChangePolicy.protectedPath("atf-core/src/main/java/Runtime.java"));
        assertTrue(TestChangePolicy.protectedPath("atf-test-practice-shop-other/src/test/OtherTest.java"));
        assertFalse(TestChangePolicy.validate(List.of(new GitTestChanges.Change("A",
                "atf-core/src/test/weird?name.java")), List.of(), BASE, TODAY).isEmpty());
    }

    @Test
    void parsesBoundedRequestAndRejectsMalformedRequests() throws Exception {
        assertEquals(Set.of(PATH), FeedbackTestChangeRequests.parse(markdown(PATH), "request.md").getFirst().paths());
        for (String path : List.of("atf-core/src/test/**", "../atf-core/src/test/T.java", "/tmp/T.java",
                "atf-core/./src/test/T.java", "atf-core\\src\\test\\T.java")) {
            assertThrows(IOException.class, () -> FeedbackTestChangeRequests.parse(markdown(path), "bad.md"));
        }
        for (String malformed : List.of(markdown(PATH).replace("reason:", "typo:"),
                markdown(PATH).replace("status: ACTIVE", "status: OPEN"),
                markdown(PATH).replace("proof: mvn test", "proof: ''"),
                markdown(PATH).replace(BASE, "HEAD"), markdown(PATH).replace("expires: 2026-11-01", "expires: never"),
                markdown(PATH).replace("status: ACTIVE", "status: ACTIVE\n    status: CLOSED"))) {
            assertThrows(IOException.class, () -> FeedbackTestChangeRequests.parse(malformed, "bad.md"));
        }
    }

    @Test
    void readsOwnerFeedbackAndCollapsedVerifiedControlsWithoutDedicatedActiveRegistry(@TempDir Path temp)
            throws Exception {
        Path feedback = Files.createDirectories(temp.resolve("docs/feedback"));
        Path active = feedback.resolve("ATF_Reporting_Feedback.md");
        Path baseline = feedback.resolveSibling("ATF_Test_Changes_Baseline.md");
        String verified = markdown(PATH).replace("CHECK-01", "VERIFIED-01")
                .replace("status: ACTIVE", "status: VERIFIED");
        Files.writeString(active, markdown(PATH));
        Files.writeString(baseline, "<details>\n<summary>Verified controls</summary>\n\n"
                + verified + "\n</details>\n");
        Files.writeString(feedback.resolve("ATF_Unrelated_Feedback.md"), "# Other gaps\n");

        var controls = FeedbackTestChangeRequests.read(feedback);
        assertEquals(Set.of("CHECK-01", "VERIFIED-01"), controls.stream()
                .map(TestChangeRequest::id).collect(toSet()));
        assertEquals(Set.of("feedback/ATF_Reporting_Feedback.md", "ATF_Test_Changes_Baseline.md"),
                controls.stream().map(TestChangeRequest::source)
                        .collect(toSet()));
        assertEquals(Set.of("ACTIVE", "VERIFIED"), controls.stream()
                .map(TestChangeRequest::status).collect(toSet()));

        Files.delete(active);
        assertEquals(List.of("VERIFIED-01"), FeedbackTestChangeRequests.read(feedback).stream()
                .map(TestChangeRequest::id).toList(), "owner feedback may omit an ACTIVE request while baseline controls remain");

        Files.writeString(active, verified);
        IOException duplicate = assertThrows(IOException.class, () -> FeedbackTestChangeRequests.read(feedback));
        assertTrue(duplicate.getMessage().contains("TEST-CHANGE-ID: duplicate VERIFIED-01"));
    }

    @Test
    void duplicateIdsAndUnclosedFencesFailClosed(@TempDir Path temp) throws Exception {
        Files.writeString(temp.resolve("a.md"), markdown(PATH));
        Files.writeString(temp.resolve("b.md"), markdown(PATH));
        assertThrows(IOException.class, () -> FeedbackTestChangeRequests.read(temp));
        assertThrows(IOException.class, () -> FeedbackTestChangeRequests.parse(
                markdown(PATH).substring(0, markdown(PATH).lastIndexOf("```")), "bad.md"));
    }

    @Test
    void malformedAuthorizationCannotHideBehindAValidBlock() {
        String valid = markdown(PATH);
        String unclosed = valid.substring(0, valid.lastIndexOf("```"));
        for (String invalid : List.of(valid + unclosed, valid + "\ntestChanges: []\n",
                valid.replace("\n```", "\n---\ntestChanges: []\n```"),
                valid.replace("paths:", "allowDelete: 'true'\n    paths:"),
                valid.replace("proof: mvn test", "proof: 123"),
                valid.replace("      - '" + PATH + "'", "      - '" + PATH + "'\n      - '" + PATH + "'"))) {
            assertThrows(IOException.class, () -> FeedbackTestChangeRequests.parse(invalid, "bad.md"));
        }
    }

    private static List<String> check(String kind, List<TestChangeRequest> requests) {
        return TestChangePolicy.validate(List.of(new GitTestChanges.Change(kind, PATH)), requests, BASE, TODAY);
    }

    private static TestChangeRequest request(String status, String base, LocalDate expires, boolean deletion) {
        return new TestChangeRequest("CHECK-01", status, base, expires,
                "Fix a documented component contract", "mvn test", Set.of(PATH), deletion, "check.md");
    }

    private static String markdown(String path) {
        return """
                ```yaml
                testChanges:
                  - id: CHECK-01
                    status: ACTIVE
                    base: %s
                    expires: 2026-11-01
                    reason: Fix a documented component contract
                    proof: mvn test
                    paths:
                      - '%s'
                ```
                """.formatted(BASE, path);
    }
}
