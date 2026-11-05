package atf.testkit.documentation.guides;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

/** Compiler input is explicit: the README wrapper and Maven runtime JARs, with no source fallback. */
public record GuideCompilation(boolean passed, String diagnostics, Path classes) {
    static GuideCompilation compile(Path work, String source, List<Path> classpath) throws Exception {
        Path sources = Files.createDirectories(work.resolve("source")), classes = Files.createDirectories(work.resolve("classes"));
        Path emptySources = Files.createDirectories(work.resolve("empty-sourcepath"));
        Path file = sources.resolve("GuideExample.java");
        Files.writeString(file, source);
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("A Java 21 JDK is required for guide acceptance");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var manager = compiler.getStandardFileManager(diagnostics, java.util.Locale.ROOT, java.nio.charset.StandardCharsets.UTF_8)) {
            String dependencies = String.join(java.io.File.pathSeparator, classpath.stream().map(Path::toString).toList());
            var options = List.of("--release", "21", "-encoding", "UTF-8", "-proc:none", "-implicit:none",
                    "-sourcepath", emptySources.toString(), "-classpath", dependencies, "-d", classes.toString());
            boolean passed = compiler.getTask(null, manager, diagnostics, options, null, manager.getJavaFileObjects(file)).call();
            String detail = diagnostics.getDiagnostics().toString();
            Files.writeString(work.resolve("compile.log"), detail);
            return new GuideCompilation(passed, detail, classes);
        }
    }
}
