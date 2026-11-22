package atf.api.tests.governance.documentation.language;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Reads the branch-local documentation language contract. */
public final class DocumentationLanguagePolicy {
    static final String POLICY_PATH = "config/atf-governance-policy.json";
    private static final List<String> SUPPORTED_LANGUAGES = List.of("en", "ro");

    private DocumentationLanguagePolicy() {
    }

    public static String read(Path root) throws IOException {
        Path policyPath = root.resolve(POLICY_PATH);
        if (!Files.isRegularFile(policyPath)) {
            throw new IOException("DOC-LANGUAGE-CONFIG: missing " + POLICY_PATH);
        }

        JsonNode policy = new ObjectMapper().readTree(policyPath.toFile());
        if (policy == null || !policy.isObject() || policy.path("schemaVersion").asInt(-1) != 1) {
            throw new IOException("DOC-LANGUAGE-CONFIG: expected schemaVersion 1 object");
        }
        JsonNode language = policy.get("documentationLanguage");
        if (language == null || !language.isTextual()
                || !SUPPORTED_LANGUAGES.contains(language.textValue())) {
            throw new IOException("DOC-LANGUAGE-CONFIG: documentationLanguage must be en or ro");
        }
        return language.textValue();
    }

    public static String orientationHeading(String language) {
        return switch (language) {
            case "en" -> "At a glance";
            case "ro" -> "Pe scurt";
            default -> throw new IllegalArgumentException("Unsupported documentation language: " + language);
        };
    }
}
