package atf.api.tests.governance.documentation.links;

import atf.api.tests.governance.documentation.DocumentationGovernanceSupport;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Checks J0's owned GitHub routes against the explicitly adjacent source checkouts, without network access. */
public final class WorkspaceDocumentationLinks {
    private static final Pattern LINK = Pattern.compile("\\]\\(([^)]+)\\)");
    private static final Map<String, String> REPOSITORIES = Map.of(
            "atf_j0", "atf_j0", "atf_j1", "atf_j1", "atf_j2", "atf_j2", "atf_j3", "atf_j3",
            "whist", "atf_j4", "rentz", "atf_j5", "atf_j6", "atf_j6", "chess", "atf_j7");
    private static final Set<String> REFS = Set.of("HEAD", "main", "main_RO", "master");

    private WorkspaceDocumentationLinks() { }

    public static void validate(Path source, Path root, List<String> lines,
                                List<DocumentationGovernanceSupport.Violation> violations) {
        List<String> visible = MarkdownAnchorIndex.withoutFences(lines);
        for (int index = 0; index < visible.size(); index++) {
            var matcher = LINK.matcher(visible.get(index));
            while (matcher.find()) {
                String destination = matcher.group(1).trim().replaceFirst("^<", "").replaceFirst(">$", "");
                try {
                    if (destination.matches("^(?:\\.\\./)+atf_j[0-7]/.*")) {
                        reject(source, root, index + 1, destination, "DOC-WORKSPACE-LINK",
                                "a sibling checkout path is not a portable GitHub route", violations);
                    } else if (destination.startsWith("https://github.com/crististefanita/")) {
                        inspectOwnedRoute(source, root, index + 1, destination, violations);
                    }
                } catch (Exception invalid) {
                    reject(source, root, index + 1, destination, "DOC-WORKSPACE-LINK",
                            "owned route cannot be inspected: " + invalid.getMessage(), violations);
                }
            }
        }
    }

    private static void inspectOwnedRoute(Path source, Path root, int line, String destination,
                                          List<DocumentationGovernanceSupport.Violation> violations) throws Exception {
        URI uri = URI.create(destination);
        List<String> parts = Arrays.stream(uri.getPath().split("/")).filter(part -> !part.isEmpty()).toList();
        if (parts.size() < 2) return;
        String repository = REPOSITORIES.get(parts.get(1));
        if (repository == null) {
            if (parts.get(1).matches("atf_j[0-7]")) reject(source, root, line, destination,
                    "DOC-WORKSPACE-REPOSITORY", "workspace identity is not its published repository name", violations);
            return;
        }
        Path checkout = repository.equals("atf_j0") ? root : root.resolveSibling(repository);
        checkout = checkout.toAbsolutePath().normalize();
        Path target;
        if (parts.size() == 2) {
            target = checkout.resolve("README.md");
        } else if (parts.size() >= 5 && Set.of("blob", "tree").contains(parts.get(2)) && REFS.contains(parts.get(3))) {
            target = checkout.resolve(String.join("/", parts.subList(4, parts.size()))).normalize();
        } else {
            reject(source, root, line, destination, "DOC-WORKSPACE-ROUTE", "unsupported document route or ref", violations);
            return;
        }
        if (!target.startsWith(checkout) || !Files.exists(target)) {
            reject(source, root, line, destination, "DOC-WORKSPACE-TARGET", "owned document target is missing", violations);
            return;
        }
        if (Files.isDirectory(target)) target = target.resolve("README.md");
        String rawFragment = uri.getRawFragment();
        if (rawFragment != null && !rawFragment.isEmpty() && target.getFileName().toString().endsWith(".md")) {
            String fragment = URLDecoder.decode(rawFragment.replace("+", "%2B"), StandardCharsets.UTF_8);
            if (!Files.isRegularFile(target) || !MarkdownAnchorIndex.anchors(Files.readAllLines(target)).contains(fragment)) {
                reject(source, root, line, destination, "DOC-WORKSPACE-ANCHOR",
                        "owned Markdown fragment is missing outside literal examples", violations);
            }
        }
    }

    private static void reject(Path source, Path root, int line, String destination, String code, String reason,
                               List<DocumentationGovernanceSupport.Violation> violations) {
        violations.add(DocumentationGovernanceSupport.violation(code, root, source, line,
                reason + ": " + destination,
                "owned cross-repository links use published GitHub names and current document paths/fragments",
                "correct the GitHub route from docs/workspaces/README.md or restore its actual owner target"));
    }
}
