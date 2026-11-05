package atf.testkit.documentation.guides;

import java.util.List;
import java.util.regex.Pattern;

/** Extracts executable examples from the delivered guide, never from a parallel Java fixture. */
public record GuideMarkdown(String text) {
    public String block(String language) {
        var matcher = Pattern.compile("(?m)^```" + Pattern.quote(language) + "\\s*\\R([\\s\\S]*?)^```\\s*$").matcher(text);
        var blocks = matcher.results().map(result -> result.group(1)).toList();
        if (blocks.size() != 1 || blocks.getFirst().isBlank())
            throw new IllegalArgumentException("Expected exactly one nonempty " + language + " example");
        return blocks.getFirst();
    }

    public String executable(boolean method, String oracle, String support) {
        List<String> lines = block("java").lines().toList();
        String imports = String.join("\n", lines.stream().filter(line -> line.stripLeading().startsWith("import ")).toList());
        String body = String.join("\n", lines.stream().filter(line -> !line.stripLeading().startsWith("import ")).toList());
        String main = "public static void main(String[] args) throws Exception {\nassertBinary();\n";
        return imports + "\npublic final class GuideExample {\n"
                + (method ? body + "\n" + main + oracle : main + body + "\n" + oracle) + "\n}\n" + support + "\n}\n";
    }
}
