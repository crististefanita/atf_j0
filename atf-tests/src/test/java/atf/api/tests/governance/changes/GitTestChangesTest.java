package atf.api.tests.governance.changes;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
public class GitTestChangesTest {
    private static final String TRACKED_EDIT = "atf-core/src/test/Edited.java";
    private static final String TRACKED_DELETE = "atf-core/src/test/Deleted.java";
    private static final String STAGED_ADDITION = "atf-core/src/test/Added.java";
    private static final String BASELINE_CONTENT = "baseline";

    @Test
    void realDiffFailsUntilTheMatchingChangeControlExistsAndCannotReuseAnOldBase(@TempDir Path root)
            throws Exception {
        GitTestChanges.command(root, "init", "--quiet");
        GitTestChanges.command(root, "config", "user.name", "Guard Fixture");
        GitTestChanges.command(root, "config", "user.email", "guard@example.test");
        String relative = "atf-core/src/test/ContractTest.java";
        Path test = root.resolve(relative);
        Files.createDirectories(test.getParent());
        Files.writeString(test, "baseline");
        Path feedback = Files.createDirectories(root.resolve("docs/feedback"));
        Files.writeString(feedback.resolve("README.md"), "# Feedback\n");
        GitTestChanges.command(root, "add", ".");
        GitTestChanges.command(root, "-c", "core.hooksPath=", "commit", "--quiet", "--no-gpg-sign", "-m", "baseline");
        var initial = GitTestChanges.read(root, "HEAD");
        Files.writeString(test, "modified");
        var changed = GitTestChanges.read(root, "HEAD");
        LocalDate today = LocalDate.now();
        assertFalse(TestChangePolicy.validate(changed.changes(), FeedbackTestChangeRequests.read(feedback),
                changed.base(), today).isEmpty());
        Path todo = feedback.resolve("request.md");
        Files.writeString(todo, """
                ```yaml
                testChanges:
                  - id: REAL-GIT-01
                    status: ACTIVE
                    base: %s
                    expires: %s
                    reason: Prove that a test edit needs a matching feedback request.
                    proof: mvn test
                    paths: [%s]
                ```
                """.formatted(initial.base(), today.plusDays(2), relative));
        assertTrue(TestChangePolicy.validate(changed.changes(), FeedbackTestChangeRequests.read(feedback),
                changed.base(), today).isEmpty());
        GitTestChanges.command(root, "add", ".");
        GitTestChanges.command(root, "-c", "core.hooksPath=", "commit", "--quiet", "--no-gpg-sign", "-m", "integrated");
        Files.writeString(test, "a later unrelated modification");
        var later = GitTestChanges.read(root, "HEAD");
        assertFalse(TestChangePolicy.validate(later.changes(), FeedbackTestChangeRequests.read(feedback),
                later.base(), today).isEmpty());
    }

    @Test
    void observesStagedUnstagedUntrackedAndCommittedChanges(@TempDir Path root) throws Exception {
        GitTestChanges.command(root, "init", "--quiet");
        GitTestChanges.command(root, "config", "user.name", "Guard Fixture");
        GitTestChanges.command(root, "config", "user.email", "guard@example.test");
        Path tests = Files.createDirectories(root.resolve("atf-core/src/test"));
        for (String name : new String[]{"Staged.java", "Unstaged.java", "Deleted.java", "Renamed.java"}) {
            Files.writeString(tests.resolve(name), "baseline");
        }
        GitTestChanges.command(root, "add", ".");
        GitTestChanges.command(root, "-c", "core.hooksPath=", "commit", "--quiet", "--no-gpg-sign", "-m", "baseline");
        String base = GitTestChanges.command(root, "rev-parse", "HEAD").trim();
        Files.writeString(tests.resolve("Staged.java"), "changed");
        GitTestChanges.command(root, "add", ".");
        GitTestChanges.command(root, "-c", "core.hooksPath=", "commit", "--quiet", "--no-gpg-sign", "-m", "committed test edit");
        Files.writeString(tests.resolve("Unstaged.java"), "unstaged");
        Files.delete(tests.resolve("Deleted.java"));
        Files.move(tests.resolve("Renamed.java"), tests.resolve("New name.java"));
        Files.writeString(tests.resolve("Added.java"), "staged");
        GitTestChanges.command(root, "add", "atf-core/src/test/Added.java");
        Files.writeString(root.resolve(".gitignore"),
                "atf-core/src/test/Ignored.java\npom.xml\nnew-module/pom.xml\n");
        Files.writeString(tests.resolve("Ignored.java"), "ignored but still compiled by Maven");
        Files.writeString(root.resolve("pom.xml"), "ignored root build configuration");
        Path ignoredModule = Files.createDirectories(root.resolve("new-module"));
        Files.writeString(ignoredModule.resolve("pom.xml"), "ignored module build configuration");

        var changes = GitTestChanges.read(root, base).changes();
        assertTrue(changes.contains(new GitTestChanges.Change("M", "atf-core/src/test/Staged.java")));
        assertTrue(changes.contains(new GitTestChanges.Change("M", "atf-core/src/test/Unstaged.java")));
        assertTrue(changes.contains(new GitTestChanges.Change("D", "atf-core/src/test/Deleted.java")));
        assertTrue(changes.contains(new GitTestChanges.Change("D", "atf-core/src/test/Renamed.java")));
        assertTrue(changes.contains(new GitTestChanges.Change("A", "atf-core/src/test/New name.java")));
        assertTrue(changes.contains(new GitTestChanges.Change("A", "atf-core/src/test/Added.java")));
        assertTrue(changes.contains(new GitTestChanges.Change("A", "atf-core/src/test/Ignored.java")));
        for (String path : new String[]{"pom.xml", "new-module/pom.xml"}) {
            assertTrue(changes.contains(new GitTestChanges.Change("A", path)), path);
            assertTrue(TestChangePolicy.validate(changes, List.of(), base, LocalDate.now())
                    .stream().anyMatch(violation -> violation.contains(" " + path + ":")),
                    "ignored build configuration must require an explicit change control: " + path);
        }
    }

    @Test
    void missingGitAndInvalidBaselineFailInsteadOfSkipping(@TempDir Path root) throws Exception {
        assertThrows(IOException.class, () -> GitTestChanges.read(root, "HEAD"));
        GitTestChanges.command(root, "init", "--quiet");
        assertThrows(IOException.class, () -> GitTestChanges.read(root, "no-such-ref"));
    }

    @Test
    void stagedChangesRemainVisibleWhenWorkingTreeHidesThem(@TempDir Path root) throws Exception {
        String base = createBaseline(root);
        Path edited = root.resolve(TRACKED_EDIT);
        Path deleted = root.resolve(TRACKED_DELETE);
        Path added = root.resolve(STAGED_ADDITION);
        Files.writeString(edited, "staged edit");
        Files.delete(deleted);
        Files.writeString(added, "staged addition");
        GitTestChanges.command(root, "add", "--all");

        Files.writeString(edited, BASELINE_CONTENT);
        Files.writeString(deleted, BASELINE_CONTENT);
        Files.delete(added);

        var snapshot = GitTestChanges.read(root, base);
        var changes = snapshot.changes();
        assertAll(
                () -> assertTrue(changes.contains(new GitTestChanges.Change("M", TRACKED_EDIT)),
                        "the index edit must survive restoration of the working file"),
                () -> assertTrue(changes.contains(new GitTestChanges.Change("D", TRACKED_DELETE)),
                        "recreating a file must not conceal its staged deletion"),
                () -> assertTrue(changes.contains(new GitTestChanges.Change("A", STAGED_ADDITION)),
                        "removing the working file must not conceal its staged addition"),
                () -> assertEquals(changes.size(), changes.stream().distinct().count(),
                        "identical kind/path changes from both views must be deduplicated"));

        LocalDate today = LocalDate.now();
        Set<String> paths = Set.of(TRACKED_EDIT, TRACKED_DELETE, STAGED_ADDITION);
        var ordinary = new TestChangeRequest("INDEX-01", "ACTIVE", base, today,
                "Review index and working tree independently", "mvn test", paths, false, "index.md");
        var violations = TestChangePolicy.validate(changes, List.of(ordinary), base, today);
        assertEquals(1, violations.size(), violations.toString());
        assertTrue(violations.getFirst().contains("D " + TRACKED_DELETE));
        var deletionApproved = new TestChangeRequest("INDEX-01", "ACTIVE", base, today,
                ordinary.reason(), ordinary.proof(), paths, true, "index.md");
        assertTrue(TestChangePolicy.validate(changes, List.of(deletionApproved), base, today).isEmpty());
    }

    @Test
    void committedEditRemainsVisibleWhenWorkingTreeMatchesReviewBase(@TempDir Path root) throws Exception {
        String base = createBaseline(root);
        Path edited = root.resolve(TRACKED_EDIT);
        Files.writeString(edited, "committed edit");
        GitTestChanges.command(root, "add", "--all");
        GitTestChanges.command(root, "-c", "core.hooksPath=", "commit", "--quiet", "--no-gpg-sign",
                "-m", "committed edit");
        Files.writeString(edited, BASELINE_CONTENT);

        var snapshot = GitTestChanges.read(root, base);

        assertEquals(List.of(new GitTestChanges.Change("M", TRACKED_EDIT)), snapshot.changes());
        assertFalse(TestChangePolicy.validate(snapshot.changes(), List.of(), base, LocalDate.now()).isEmpty(),
                "a working-tree rollback cannot authorize the committed edit");
    }

    private static String createBaseline(Path root) throws Exception {
        GitTestChanges.command(root, "init", "--quiet");
        GitTestChanges.command(root, "config", "user.name", "Guard Fixture");
        GitTestChanges.command(root, "config", "user.email", "guard@example.test");
        for (String relative : List.of(TRACKED_EDIT, TRACKED_DELETE)) {
            Path file = root.resolve(relative);
            Files.createDirectories(file.getParent());
            Files.writeString(file, BASELINE_CONTENT);
        }
        GitTestChanges.command(root, "add", "--all");
        GitTestChanges.command(root, "-c", "core.hooksPath=", "commit", "--quiet", "--no-gpg-sign",
                "-m", "baseline");
        return GitTestChanges.command(root, "rev-parse", "HEAD").trim();
    }

    @Test
    void ciRequiresAnExplicitReviewBaseWhileLocalIntelliJDefaultsToHead() {
        assertEquals("HEAD", GitTestChanges.baseRef(null, Map.of()));
        assertEquals("review-base", GitTestChanges.baseRef(null,
                Map.of("CI", "true", GitTestChanges.BASE_ENV, "review-base")));
        assertEquals("property-base", GitTestChanges.baseRef("property-base",
                Map.of(GitTestChanges.BASE_ENV, "environment-base")));
        assertThrows(IllegalStateException.class, () -> GitTestChanges.baseRef(null, Map.of("CI", "true")));
        assertThrows(IllegalStateException.class, () -> GitTestChanges.baseRef("HEAD", Map.of("CI", "true")));
    }
}
