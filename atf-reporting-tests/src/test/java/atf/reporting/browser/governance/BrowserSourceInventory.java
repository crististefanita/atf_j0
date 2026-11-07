package atf.reporting.browser.governance;

import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import java.nio.file.*;
import java.util.*;
import javax.lang.model.element.Modifier;
import javax.tools.ToolProvider;

/** Typed JDK inventory: strings/comments cannot masquerade as SDK calls or test annotations. */
final class BrowserSourceInventory {
    record Entry(String name, String packageName, String parent, boolean annotation, boolean abstractType,
                 Set<String> annotations, Set<String> references, List<String> imports,
                 boolean ownTests, boolean factory, boolean explicitWorker, List<String> violations) { }
    private static final String OWNER = "atf.ui.tests.extent.support.lifecycle.BrowserResources";

    static Map<String, Entry> read(Path source) throws Exception {
        var result = new LinkedHashMap<String, Entry>();
        try (var manager = ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null);
             var paths = Files.walk(source)) {
            var files = paths.filter(path -> path.toString().endsWith(".java")).toList();
            var task = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, manager, null,
                    List.of("-proc:none", "-classpath", System.getProperty("java.class.path")), null, manager.getJavaFileObjectsFromPaths(files));
            var units = new ArrayList<CompilationUnitTree>(); task.parse().forEach(units::add);
            task.analyze();
            Trees trees = Trees.instance(task);
            for (CompilationUnitTree unit : units) {
                String pkg = unit.getPackageName().toString();
                var imports = unit.getImports().stream().map(value -> value.getQualifiedIdentifier().toString()).toList();
                for (Tree declaration : unit.getTypeDecls()) if (declaration instanceof ClassTree type) {
                    String name = pkg + "." + type.getSimpleName();
                    var references = new LinkedHashSet<String>();
                    var violations = new ArrayList<String>();
                    var flags = new boolean[3];
                    new TreePathScanner<Void, Void>() {
                        @Override public Void visitIdentifier(IdentifierTree item, Void unused) { references.add(item.getName().toString()); return super.visitIdentifier(item, unused); }
                        @Override public Void visitMemberSelect(MemberSelectTree item, Void unused) { references.add(item.getIdentifier().toString()); return super.visitMemberSelect(item, unused); }
                        @Override public Void visitMethod(MethodTree method, Void unused) {
                            if (method.getModifiers().getAnnotations().stream().anyMatch(value -> Set.of("Test", "ParameterizedTest", "RepeatedTest", "TestFactory")
                                    .contains(simple(value.getAnnotationType().toString())))) flags[0] = true;
                            return super.visitMethod(method, unused);
                        }
                        @Override public Void visitMethodInvocation(MethodInvocationTree call, Void unused) {
                            String select = call.getMethodSelect().toString();
                            var method = trees.getElement(getCurrentPath());
                            String declared = method == null ? "" : method.getEnclosingElement().toString();
                            references.add(declared);
                            if (select.startsWith("BrowserResources.") && Set.of("create", "open", "launch", "context", "newPage").contains(simple(select))) flags[1] = true;
                            if (select.equals("BrowserProbeProcess.run")) flags[2] = true;
                            boolean raw = select.equals("Playwright.create") || select.endsWith(".newPage") || select.endsWith(".newContext") || select.matches(".*\\.chromium\\(\\)\\.launch");
                            if (!name.equals(OWNER) && raw && !select.startsWith("BrowserResources.")) violations.add("Unbounded SDK acquisition: " + name + " -> " + select);
                            if (select.equals("PlaywrightUiSession.open") && !insideCreate()) violations.add("Framework browser acquisition is not bounded: " + name);
                            if (!name.equals(OWNER) && declared.startsWith("com.microsoft.playwright.")
                                    && Set.of("close", "dispose", "launch", "launchPersistentContext", "connect", "connectOverCDP", "newPage", "newContext", "create")
                                    .contains(method.getSimpleName().toString())) violations.add("SDK lifecycle bypass: " + name + " -> " + select);
                            return super.visitMethodInvocation(call, unused);
                        }
                        @Override public Void visitTry(TryTree block, Void unused) {
                            boolean needsGuard = false;
                            for (Tree resource : block.getResources()) {
                                var mirror = trees.getTypeMirror(new TreePath(getCurrentPath(), resource));
                                if (mirror != null && (mirror.toString().startsWith("atf.reporting.browser.lifecycle.BrowserLease")
                                        || mirror.toString().equals("atf.ui.playwright.PlaywrightUiSession")
                                        || mirror.toString().equals("atf.ui.tests.extent.support.audit.ReportAuditContext"))) needsGuard = true;
                                if (!name.equals(OWNER) && mirror != null && mirror.toString().startsWith("com.microsoft.playwright."))
                                    violations.add("Raw SDK try-with-resource bypass: " + name + " -> " + resource);
                            }
                            if (needsGuard && !name.equals(OWNER)) {
                                var statements = block.getBlock().getStatements();
                                boolean guarded = !statements.isEmpty() && statements.getFirst() instanceof TryTree guard
                                        && guard.getCatches().stream().anyMatch(handler -> handler.getBlock().getStatements().stream()
                                        .anyMatch(statement -> statement instanceof ExpressionStatementTree expression
                                                && expression.getExpression() instanceof MethodInvocationTree call
                                                && call.getMethodSelect().toString().equals("BrowserResources.recordFailure")));
                                if (!guarded) violations.add("Browser body can lose its original failure during disposal: " + name);
                            }
                            return super.visitTry(block, unused);
                        }
                        private boolean insideCreate() {
                            for (var path = getCurrentPath().getParentPath(); path != null; path = path.getParentPath())
                                if (path.getLeaf() instanceof MethodInvocationTree call && call.getMethodSelect().toString().equals("BrowserResources.create")) return true;
                            return false;
                        }
                    }.scan(unit, null);
                    var annotations = new LinkedHashSet<String>();
                    type.getModifiers().getAnnotations().forEach(value -> annotations.add(simple(value.getAnnotationType().toString())));
                    result.put(name, new Entry(name, pkg, type.getExtendsClause() == null ? "" : simple(type.getExtendsClause().toString()),
                            type.getKind() == Tree.Kind.ANNOTATION_TYPE, type.getModifiers().getFlags().contains(Modifier.ABSTRACT),
                            annotations, references, imports, flags[0], flags[1], flags[2], violations));
                }
            }
        }
        return result;
    }

    static String resolve(Entry owner, String simple, Map<String, Entry> entries) {
        if (entries.containsKey(simple)) return simple;
        if (entries.containsKey(owner.packageName() + "." + simple)) return owner.packageName() + "." + simple;
        for (String imported : owner.imports()) {
            String candidate = imported.endsWith(".*") ? imported.substring(0, imported.length() - 1) + simple : imported;
            if (candidate.endsWith("." + simple) && entries.containsKey(candidate)) return candidate;
        }
        var candidates = entries.keySet().stream().filter(name -> name.endsWith("." + simple)).toList();
        return candidates.size() == 1 ? candidates.getFirst() : "";
    }

    static boolean tests(Entry entry, Map<String, Entry> entries, Set<String> seen) {
        if (entry == null || !seen.add(entry.name())) return false;
        return entry.ownTests() || tests(entries.get(resolve(entry, entry.parent(), entries)), entries, seen);
    }

    static boolean browser(Entry entry, Map<String, Entry> entries, Set<String> seen) {
        if (entry == null || entry.annotation() || entry.explicitWorker() || !seen.add(entry.name())) return false;
        if (entry.factory()) return true;
        for (String reference : entry.references())
            if (browser(entries.get(resolve(entry, reference, entries)), entries, seen)) return true;
        return false;
    }

    private static String simple(String value) { int dot = value.lastIndexOf('.'); return dot < 0 ? value : value.substring(dot + 1); }
}
