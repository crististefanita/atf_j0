package atf.api.tests.governance.changes;

import atf.core.config.module.AtfModuleIds;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class TestChangePolicy {
    private static final String POM_FILE = "pom.xml";

    private TestChangePolicy() { }

    static boolean isExactPath(String value) {
        if (value == null || value.isBlank() || value.contains("\\") || value.contains(":")
                || value.contains("*") || value.contains("?") || value.startsWith("/")) return false;
        try {
            Path path = Path.of(value);
            return !path.isAbsolute() && (path.getNameCount() > 1 || POM_FILE.equals(value))
                    && path.normalize().toString().replace('\\', '/').equals(value)
                    && !value.startsWith("../");
        } catch (java.nio.file.InvalidPathException invalid) {
            return false;
        }
    }

    static boolean protectedPath(String path) {
        if (POM_FILE.equals(path)) return true;
        if (path == null || path.indexOf('/') <= 0) return false;
        int slash = path.indexOf('/');
        String module = path.substring(0, slash);
        if (module.equals(AtfModuleIds.PRACTICE_SHOP)) return false;
        String relative = path.substring(slash + 1);
        return relative.startsWith("src/test/") || POM_FILE.equals(relative);
    }

    static List<String> validate(List<GitTestChanges.Change> changes, List<TestChangeRequest> requests,
                                  String base, LocalDate today) {
        var violations = new ArrayList<String>();
        for (var request : requests) {
            for (String path : request.paths()) {
                if (!protectedPath(path)) violations.add("TEST-CHANGE-SCOPE " + request.source()
                        + ": " + request.id() + " does not name a protected test path: " + path);
            }
        }
        for (var change : changes) {
            if (!protectedPath(change.path())) continue;
            if (!isExactPath(change.path())) {
                violations.add("TEST-CHANGE-PATH " + change.path() + ": invalid protected path must not bypass governance");
                continue;
            }
            if (!Set.of("A", "M", "D", "T").contains(change.kind())) {
                violations.add("TEST-CHANGE-CONFLICT " + change.path() + ": resolve Git state " + change.kind());
                continue;
            }
            var eligible = requests.stream().filter(request -> request.paths().contains(change.path()))
                    .filter(request -> Set.of("ACTIVE", "VERIFIED").contains(request.status()))
                    .filter(request -> request.base().equals(base))
                    .filter(request -> !request.expires().isBefore(today))
                    .filter(request -> !change.kind().equals("D") || request.allowDelete()).toList();
            if (eligible.isEmpty()) {
                violations.add("TEST-CHANGE-REQUIRED " + change.kind() + " " + change.path()
                        + ": add an ACTIVE testChanges control to the relevant owner document in docs/feedback/**/*.md"
                        + " or an eligible VERIFIED change control in "
                        + "docs/ATF_Test_Changes_Baseline.md with this exact path,"
                        + " base=" + base + ", unexpired date, reason and proof; deletions require allowDelete: true");
            } else if (eligible.size() > 1) {
                violations.add("TEST-CHANGE-AMBIGUOUS " + change.path() + ": multiple eligible change controls "
                        + eligible.stream().map(TestChangeRequest::id).toList());
            }
        }
        return List.copyOf(violations);
    }
}
