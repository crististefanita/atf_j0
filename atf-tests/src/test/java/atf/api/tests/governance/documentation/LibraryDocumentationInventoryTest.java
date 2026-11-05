package atf.api.tests.governance.documentation;

import atf.testkit.AtfUnitTest;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class LibraryDocumentationInventoryTest {
    @Test void onlyNamedRegularLibraryGuidesJoinTheDocumentationInventory(@TempDir Path root) throws Exception {
        Path lib = Files.createDirectory(root.resolve("lib"));
        for (String name : List.of("README.md", "README.Core.md", "README.Reporting.md", "README.Console.md", "README.Mock.md")) {
            Path file = lib.resolve(name); Files.writeString(file, "# Ghid\n");
            assertTrue(DocumentationGovernanceSupport.isGovernedMarkdown(root, file), name);
        }
        Path unknown = lib.resolve("README.Other.md"); Files.writeString(unknown, "# Alte date\n");
        assertFalse(DocumentationGovernanceSupport.isGovernedMarkdown(root, unknown));
        Path generated = Files.createDirectories(root.resolve("target/lib")).resolve("README.Core.md"); Files.writeString(generated, "# Generat\n");
        assertFalse(DocumentationGovernanceSupport.isGovernedMarkdown(root, generated));
        assertFalse(DocumentationGovernanceSupport.isGovernedMarkdown(root.resolve("other-project"), lib.resolve("README.Core.md")));
        Files.delete(lib.resolve("README.Core.md")); Files.createDirectory(lib.resolve("README.Core.md"));
        assertFalse(DocumentationGovernanceSupport.isGovernedMarkdown(root, lib.resolve("README.Core.md")));
    }
}
