package atf.testkit.governance;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Scans maintained Java and JavaScript source for high-confidence Romanian words. */
public final class AtfSourceLanguageScanner {
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{M}]+");
    private static final Pattern IDENTIFIER = Pattern.compile("[\\p{L}_$][\\p{L}\\p{M}\\p{N}_$]*");
    private static final Pattern CAMEL_BOUNDARY = Pattern.compile(
            "(?<=[\\p{Ll}\\p{N}])(?=\\p{Lu})|(?<=\\p{Lu})(?=\\p{Lu}\\p{Ll})|[_$]+");
    private static final Pattern ASSERTION_CALL = Pattern.compile("\\b(assert[A-Za-z0-9_]*)\\s*\\(");
    private static final Set<String> ROMANIAN_WORDS = Set.of(
            "adauga", "adaugare", "afisare", "afiseaza", "alege", "alegere", "arata", "asigura",
            "atunci", "buton", "calculeaza", "carte", "carti", "caseta", "cerinta", "culoare",
            "descarca", "deschide", "deschidere", "dupa", "ecran", "exista", "fereastra", "fisier",
            "foloseste", "functie", "gestioneaza", "inainte", "incarca", "incepe", "inchide",
            "interfata", "joc", "joaca", "jucator", "jucatori", "linie", "lista", "locatie",
            "masina", "mana", "muta", "mutare", "nevoie", "nume", "numar", "pas", "piesa", "piese",
            "pozitie", "prima", "putem", "rand", "regula", "rezolva", "rezultat", "salveaza", "salvat",
            "schimba", "scor", "setare", "sterge", "stare", "tabla", "toate", "trebuie", "urmator",
            "urmatoare", "utilizator", "valoare", "verifica", "versiune", "vrem", "zona", "acelasi",
            "aceeasi", "acum", "aplicatie", "arhiva", "asemenea", "asteapta", "comanda", "complet",
            "continua", "corect", "curent", "definit", "diferit", "eroare", "etapa", "inregistrare",
            "modifica", "modificare", "ordine", "partea", "partida", "permis", "raspuns", "rulare",
            "selecteaza", "sesiune", "sursa", "testare", "validare");
    private static final String ROMANIAN_DIACRITICS = "ăâîșşțţĂÂÎȘŞȚŢ";

    private AtfSourceLanguageScanner() { }

    /** Returns source findings; strings and generated paths are excluded by the caller's inventory. */
    public static List<Finding> inspect(String fileName, String source) {
        if (fileName == null || source == null) throw new NullPointerException("fileName and source are required");
        Language language = languageFor(fileName);
        LexicalView view = lex(source, language);
        List<Finding> findings = new ArrayList<>();
        collect(view.code(), IDENTIFIER, Construct.IDENTIFIER, source, findings);
        collect(view.comments(), WORD, Construct.COMMENT, source, findings);
        for (Literal literal : view.literals()) {
            if (isDeveloperMessage(fileName, view.code(), literal.contentStart())) {
                collectLiteral(literal, source, findings);
            }
        }
        findings.sort(Comparator.comparingInt(Finding::line)
                .thenComparing(Finding::construct).thenComparing(Finding::token));
        return List.copyOf(findings);
    }

    private static Language languageFor(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".java")) return Language.JAVA;
        if (lower.endsWith(".js") || lower.endsWith(".mjs") || lower.endsWith(".cjs")) return Language.JAVASCRIPT;
        throw new IllegalArgumentException("Unsupported source language: " + fileName);
    }

    private static void collect(String text, Pattern pattern, Construct construct, String source,
                                List<Finding> findings) {
        int[] lineStarts = lineStarts(source);
        Matcher words = pattern.matcher(text);
        while (words.find()) {
            String token = words.group();
            for (String part : CAMEL_BOUNDARY.split(token)) {
                if (part.isBlank() || !isRomanian(part)) continue;
                findings.add(new Finding(lineAt(lineStarts, words.start()), construct, part));
            }
        }
    }

    private static void collectLiteral(Literal literal, String source, List<Finding> findings) {
        int[] starts = lineStarts(source);
        Matcher words = WORD.matcher(literal.value());
        while (words.find()) {
            String token = words.group();
            for (String part : CAMEL_BOUNDARY.split(token)) {
                if (isRomanian(part)) {
                    findings.add(new Finding(lineAt(starts, literal.contentStart() + words.start()),
                            Construct.DEVELOPER_MESSAGE, part));
                }
            }
        }
    }

    private static boolean isDeveloperMessage(String fileName, String code, int literalStart) {
        String prefix = code.substring(0, literalStart);
        Matcher assertionCalls = ASSERTION_CALL.matcher(prefix);
        int assertionStart = -1;
        String assertionName = "";
        while (assertionCalls.find()) {
            assertionStart = assertionCalls.end();
            assertionName = assertionCalls.group(1);
        }
        if (assertionStart >= 0 && assertionArgumentIndex(prefix, assertionStart) >= assertionMessageIndex(assertionName)) {
            return true;
        }
        String tail = prefix.stripTrailing();
        if (tail.matches("(?s).*(?:throw\\s+new\\s+[\\w.$]+|new\\s+[\\w.$]*(?:Exception|Error))\\s*\\(")) {
            return true;
        }
        boolean testSource = fileName.replace('\\', '/').contains("/src/test/");
        if (!testSource && tail.matches("(?s).*(?:System\\.(?:out|err)|console|logger|log)\\s*\\.\\s*"
                + "(?:print\\w*|error|warn|info|debug|trace)\\s*\\(")) {
            return true;
        }
        return tail.matches("(?s).*(?:diagnostic|errorMessage|failureReason)\\s*=\\s*");
    }

    private static int assertionArgumentIndex(String source, int afterOpeningParenthesis) {
        int depth = 1;
        int commas = 0;
        for (int index = afterOpeningParenthesis; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '(') depth++;
            else if (current == ')' && --depth == 0) return -1;
            else if (current == ',' && depth == 1) commas++;
        }
        return commas;
    }

    private static int assertionMessageIndex(String name) {
        if (name.equals("assertAll")) return 0;
        return Set.of("assertEquals", "assertNotEquals", "assertArrayEquals", "assertIterableEquals",
                "assertLinesMatch", "assertThrows", "assertInstanceOf", "assertTimeout",
                "assertTimeoutPreemptively", "assertDoesNotThrow").contains(name) ? 2 : 1;
    }

    private static boolean isRomanian(String token) {
        for (int index = 0; index < token.length(); index++) {
            if (ROMANIAN_DIACRITICS.indexOf(token.charAt(index)) >= 0) return true;
        }
        String normalized = Normalizer.normalize(token, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        return ROMANIAN_WORDS.contains(normalized);
    }

    private static LexicalView lex(String source, Language language) {
        char[] code = source.toCharArray();
        char[] comments = new char[source.length()];
        Arrays.fill(comments, ' ');
        for (int index = 0; index < source.length(); index++) {
            if (source.charAt(index) == '\n' || source.charAt(index) == '\r') comments[index] = source.charAt(index);
        }
        List<Literal> literals = new ArrayList<>();
        scanCode(source, code, comments, literals, 0, language, false);
        return new LexicalView(new String(code), new String(comments), List.copyOf(literals));
    }

    private static int scanCode(String source, char[] code, char[] comments, List<Literal> literals, int start,
                                Language language, boolean expression) {
        int braceDepth = 0;
        for (int index = start; index < source.length();) {
            char current = source.charAt(index);
            if (expression && current == '}') {
                if (braceDepth == 0) return index + 1;
                braceDepth--;
                index++;
                continue;
            }
            if (expression && current == '{') {
                braceDepth++;
                index++;
                continue;
            }
            if (current == '/' && index + 1 < source.length() && source.charAt(index + 1) == '/') {
                int end = lineEnd(source, index);
                copyComment(source, comments, code, index, end);
                index = end;
            } else if (current == '/' && index + 1 < source.length() && source.charAt(index + 1) == '*') {
                int end = blockCommentEnd(source, index);
                copyComment(source, comments, code, index, end);
                index = end;
            } else if (language == Language.JAVA && source.startsWith("\"\"\"", index)) {
                index = maskTextBlock(source, code, literals, index);
            } else if (current == '\'' || current == '"') {
                index = maskQuoted(source, code, literals, index, current, language);
            } else if (language == Language.JAVASCRIPT && current == 96) {
                index = maskTemplate(source, code, comments, literals, index);
            } else if (language == Language.JAVASCRIPT && current == '/' && regexMayStart(source, index)) {
                index = maskRegex(source, code, index);
            } else {
                index++;
            }
        }
        return source.length();
    }

    private static int maskTemplate(String source, char[] code, char[] comments, List<Literal> literals, int opening) {
        mask(code, source, opening, opening + 1);
        for (int index = opening + 1; index < source.length();) {
            char current = source.charAt(index);
            if (current == '\\') {
                int end = Math.min(source.length(), index + 2);
                mask(code, source, index, end);
                index = end;
            } else if (current == 96) {
                mask(code, source, index, index + 1);
                literals.add(new Literal(opening + 1, source.substring(opening + 1, index)));
                return index + 1;
            } else if (current == '$' && index + 1 < source.length() && source.charAt(index + 1) == '{') {
                mask(code, source, index, index + 2);
                index = scanCode(source, code, comments, literals, index + 2, Language.JAVASCRIPT, true);
            } else {
                mask(code, source, index, index + 1);
                index++;
            }
        }
        return source.length();
    }

    private static int maskTextBlock(String source, char[] code, List<Literal> literals, int opening) {
        int cursor = opening + 3;
        while (cursor < source.length()) {
            if (source.startsWith("\"\"\"", cursor) && !escaped(source, cursor)) {
                int end = cursor + 3;
                mask(code, source, opening, end);
                literals.add(new Literal(opening + 3, source.substring(opening + 3, cursor)));
                return end;
            }
            cursor++;
        }
        mask(code, source, opening, source.length());
        literals.add(new Literal(opening + 3, source.substring(opening + 3)));
        return source.length();
    }

    private static int maskQuoted(String source, char[] code, List<Literal> literals, int opening,
                                  char quote, Language language) {
        int cursor = opening + 1;
        while (cursor < source.length()) {
            char current = source.charAt(cursor);
            if (current == quote && !escaped(source, cursor)) {
                int end = cursor + 1;
                mask(code, source, opening, end);
                if (quote != '\'' || language == Language.JAVASCRIPT) {
                    literals.add(new Literal(opening + 1, source.substring(opening + 1, cursor)));
                }
                return end;
            }
            cursor++;
        }
        mask(code, source, opening, source.length());
        if (quote != '\'' || language == Language.JAVASCRIPT) {
            literals.add(new Literal(opening + 1, source.substring(opening + 1)));
        }
        return source.length();
    }

    private static int maskRegex(String source, char[] code, int opening) {
        boolean characterClass = false;
        int cursor = opening + 1;
        while (cursor < source.length()) {
            char current = source.charAt(cursor);
            if (current == '\\') {
                cursor = Math.min(source.length(), cursor + 2);
                continue;
            }
            if (current == '[') characterClass = true;
            else if (current == ']') characterClass = false;
            else if (current == '/' && !characterClass) {
                cursor++;
                while (cursor < source.length() && Character.isLetter(source.charAt(cursor))) cursor++;
                mask(code, source, opening, cursor);
                return cursor;
            }
            if (current == '\n' || current == '\r') return opening + 1;
            cursor++;
        }
        return opening + 1;
    }

    private static boolean regexMayStart(String source, int slash) {
        int cursor = slash - 1;
        while (cursor >= 0 && Character.isWhitespace(source.charAt(cursor))) cursor--;
        if (cursor < 0 || "([{:;,=!?&|+-*%^~<>".indexOf(source.charAt(cursor)) >= 0) return true;
        int end = cursor + 1;
        while (cursor >= 0 && Character.isJavaIdentifierPart(source.charAt(cursor))) cursor--;
        String previous = source.substring(cursor + 1, end);
        return Set.of("return", "throw", "case", "delete", "void", "typeof", "instanceof", "in", "of",
                "yield", "await", "else", "do").contains(previous);
    }

    private static int lineEnd(String source, int start) {
        int cursor = start;
        while (cursor < source.length() && source.charAt(cursor) != '\n' && source.charAt(cursor) != '\r') cursor++;
        return cursor;
    }

    private static int blockCommentEnd(String source, int start) {
        int end = source.indexOf("*/", start + 2);
        return end < 0 ? source.length() : end + 2;
    }

    private static void copyComment(String source, char[] comments, char[] code, int start, int end) {
        for (int index = start; index < end; index++) comments[index] = source.charAt(index);
        mask(code, source, start, end);
    }

    private static void mask(char[] target, String source, int start, int end) {
        for (int index = start; index < end; index++) {
            char current = source.charAt(index);
            if (current != '\n' && current != '\r') target[index] = ' ';
        }
    }

    private static boolean escaped(String source, int index) {
        int backslashes = 0;
        for (int cursor = index - 1; cursor >= 0 && source.charAt(cursor) == '\\'; cursor--) backslashes++;
        return backslashes % 2 != 0;
    }

    private static int[] lineStarts(String source) {
        int[] starts = new int[(int) source.chars().filter(value -> value == '\n').count() + 1];
        int next = 1;
        for (int index = 0; index < source.length(); index++) {
            if (source.charAt(index) == '\n') starts[next++] = index + 1;
        }
        return starts;
    }

    private static int lineAt(int[] starts, int offset) {
        int position = Arrays.binarySearch(starts, offset);
        return position >= 0 ? position + 1 : -position - 1;
    }

    public enum Construct { IDENTIFIER, COMMENT, DEVELOPER_MESSAGE }

    public record Finding(int line, Construct construct, String token) { }

    private enum Language { JAVA, JAVASCRIPT }

    private record LexicalView(String code, String comments, List<Literal> literals) { }

    private record Literal(int contentStart, String value) { }
}
