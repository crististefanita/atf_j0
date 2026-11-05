package atf.testkit.documentation.guides;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class GuideMarkdownTest {
    @Test void extractsTheActualJavaAndYamlWithCrLfAndPreservesTheDocumentedBody() {
        var guide = new GuideMarkdown("# Guide\r\n```java\r\nimport java.util.List;\r\nvar body = List.of(1);\r\n```\r\n```yaml\r\nflag: true\r\n```\r\n");
        assertTrue(guide.block("yaml").contains("flag: true"));
        String source = guide.executable(false, "if (body.size() != 1) throw new AssertionError();", "void support() {} ");
        assertTrue(source.indexOf("import java.util.List;") < source.indexOf("public final class GuideExample"));
        assertTrue(source.contains("var body = List.of(1);"));
    }

    @Test void absentEmptyAndAmbiguousExamplesFailClosed() {
        for (String text : new String[]{"# Missing", "```java\n \n```\n", "```java\na();\n```\n```java\nb();\n```\n"})
            assertThrows(IllegalArgumentException.class, () -> new GuideMarkdown(text).block("java"));
    }

    @Test void aMethodExampleIsNotHiddenInsideAnotherMethod() {
        String source = new GuideMarkdown("```java\npublic static void publish() {}\n```\n").executable(true, "publish();", "");
        assertTrue(source.indexOf("public static void publish()") < source.indexOf("public static void main"));
    }
}
