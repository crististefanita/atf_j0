package atf.api.tests.governance.documentation.language;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Checks visible Markdown prose while leaving code, identifiers and destinations intact. */
final class DocumentationLanguageRules {
    private static final Pattern ROMANIAN = Pattern.compile(
            "[ăâîșțşţ]|\\b(?:pe scurt|și|sau|este|sunt|pentru|fără|dacă|fiecare|înainte|după|"
                    + "păstrează|acest|aceste|această|acești|acestea|între|prin|din|despre|"
                    + "documentație|documentul|documentele|fișier|fișiere|sursă|raport|suită|"
                    + "modul|modulul|testele|verificare|arhitectură|contractul|"
                    + "rezultat|rezultatul|rulare|limită|limitele|proprietar|responsabilitate|"
                    + "dovadă|dovezi|ghid|schimbare|schimbările|curățare|"
                    + "verificarea|verificările|validarea|validării|raportul|raportului|"
                    + "contractului|contractele|contractelor|modulului|modulului|"
                    + "păstrarea|rularea|rulării|proprietarului|responsabilitatea|rezultate|"
                    + "dovezile|limitele|arhitectura|implementarea|implementării|"
                    + "pe scurt|comun|comună|utilizare|explicație|prezentare)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ROMANIAN_UNACCENTED = Pattern.compile(
            "\\b(?:si|sau|este|sunt|pentru|fara|daca|fiecare|inainte|dupa|pastreaza|"
                    + "acest|aceste|aceasta|acesti|acestea|intre|prin|documentatie|documentul|"
                    + "documentele|fisier|fisiere|sursa|raport|suita|modulul|testele|verificare|"
                    + "arhitectura|contractul|rezultatul|rulare|limita|limitele|proprietar|"
                    + "responsabilitate|dovada|dovezi|ghidul|schimbare|schimbarile|curatare|"
                    + "verificarea|verificarile|validarea|validarii|raportul|raportului|"
                    + "contractului|contractele|contractelor|modulului|pastrarea|rularea|rularii|"
                    + "proprietarului|responsabilitatea|rezultate|dovezile|arhitectura|"
                    + "implementarea|implementarii|comuna|utilizare|explicatie|prezentare)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ENGLISH_HEADING = Pattern.compile(
            "^(?:Overview|At a glance|Quick start|Getting started|Prerequisites|"
                    + "Responsibilities|Requirements|Purpose|Scope|How it works|Troubleshooting|"
                    + "Scope and cause|Scope and outcome|Reporting library and audit contracts|"
                    + "Screenshot context|Explicit fixture preparation)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^\\s*#{1,6}\\s+.*$");
    private static final Pattern ENGLISH_WORDS = Pattern.compile(
            "\\b(the|this|these|that|with|without|and|must|should|their|each|after|before|"
                    + "from|where|when|which|remain|remains|between|through|because|while|"
                    + "for|into|then|only|every|across|provides|requires|keeps)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FENCE = Pattern.compile("^\\s*(`{3,}|~{3,})(.*)$");
    private static final Pattern INLINE_CODE = Pattern.compile("`+[^`]*`+");
    private static final Pattern INLINE_DESTINATION = Pattern.compile("\\]\\((?:[^()]|\\([^()]*\\))*\\)");
    private static final Pattern URL = Pattern.compile("https?://\\S+");
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern DIAGRAM_LABEL = Pattern.compile(
            "\\[([^]\\r\\n]*)\\]|\\|([^|\\r\\n]*)\\||\\{([^}\\r\\n]*)\\}"
                    + "|\\bas\\s+(.+)$|:\\s+(.+)$", Pattern.CASE_INSENSITIVE);

    private DocumentationLanguageRules() {
    }

    static int firstForeignLine(String source, String language) {
        if (!List.of("en", "ro").contains(language)) {
            throw new IllegalArgumentException("Unsupported documentation language: " + language);
        }

        String fence = null;
        String fenceLanguage = "";
        int firstEnglishLine = 0;
        int englishWordCount = 0;
        String[] lines = source.split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            var fenceMatch = FENCE.matcher(line);
            if (fenceMatch.matches()) {
                firstEnglishLine = 0;
                englishWordCount = 0;
                if (fence == null) {
                    fence = fenceMatch.group(1);
                    fenceLanguage = fenceMatch.group(2).strip().toLowerCase(Locale.ROOT);
                } else if (fenceMatch.group(1).charAt(0) == fence.charAt(0)
                        && fenceMatch.group(1).length() >= fence.length()) {
                    fence = null;
                    fenceLanguage = "";
                }
                continue;
            }

            String visible;
            if (fence != null) {
                if (fenceLanguage.equals("mermaid")) {
                    visible = diagramLabels(line);
                } else if (fenceLanguage.equals("yaml") && line.matches("^\\s*(?:-\\s+)?reason:.*")) {
                    visible = line.replaceFirst("^\\s*(?:-\\s+)?reason:\\s*", "");
                } else {
                    continue;
                }
            } else {
                if (line.matches("^\\s*\\[[^]]+]:\\s*\\S+.*$")) {
                    firstEnglishLine = 0;
                    englishWordCount = 0;
                    continue;
                }
                visible = visibleMarkdown(line);
            }

            boolean heading = fence == null && MARKDOWN_HEADING.matcher(line).matches();
            if (language.equals("ro")) {
                if (fence == null && startsNewBlock(line, heading)) {
                    firstEnglishLine = 0;
                    englishWordCount = 0;
                }
                String headingText = visible.replaceFirst("^\\s*#{1,6}\\s+", "").strip();
                if (heading && ENGLISH_HEADING.matcher(headingText).matches()) return index + 1;
                if (firstEnglishLine == 0 && ENGLISH_WORDS.matcher(visible).find()) {
                    firstEnglishLine = index + 1;
                }
                englishWordCount += countMatches(ENGLISH_WORDS, visible);
                if (englishWordCount >= 6) return firstEnglishLine;
            } else if (isForeign(visible, language, heading)) {
                return index + 1;
            }
        }
        return 0;
    }

    private static int countMatches(Pattern pattern, String value) {
        var matcher = pattern.matcher(value);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    private static boolean startsNewBlock(String line, boolean heading) {
        return line.isBlank() || heading
                || line.matches("^\\s*(?:[-+*]|\\d+[.)])\\s+.*$")
                || line.stripLeading().startsWith("|");
    }

    private static String diagramLabels(String line) {
        var labels = DIAGRAM_LABEL.matcher(line);
        StringBuilder visible = new StringBuilder();
        while (labels.find()) {
            for (int group = 1; group <= labels.groupCount(); group++) {
                String value = labels.group(group);
                if (value != null) visible.append(value).append(' ');
            }
        }
        return visible.toString();
    }

    private static String visibleMarkdown(String line) {
        return URL.matcher(HTML_TAG.matcher(INLINE_DESTINATION.matcher(INLINE_CODE.matcher(line)
                .replaceAll(" ")).replaceAll("]")).replaceAll(" "))
                .replaceAll(" ");
    }

    private static boolean isForeign(String visible, String language, boolean heading) {
        if (language.equals("en")) {
            return ROMANIAN.matcher(visible).find() || ROMANIAN_UNACCENTED.matcher(visible).find();
        }
        String headingText = visible.replaceFirst("^\\s*#{1,6}\\s+", "").strip();
        return heading && ENGLISH_HEADING.matcher(headingText).matches()
                || ENGLISH_WORDS.matcher(visible).results()
                .map(match -> match.group().toLowerCase(Locale.ROOT)).distinct().count() >= 4;
    }
}
