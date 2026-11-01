package atf.reporting.distribution.tags;

import atf.reporting.model.ReportTagNormalizer;
import atf.testkit.AtfComponentTest;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Compiles a caller against the frozen old ABI, then loads only its bytecode against the actual library. */
@AtfComponentTest
class ReportingTagBinaryCompatibilityTest {
    @TempDir Path directory;

    @Test void aPreviouslyCompiledCallerLinksToTheActualRetainedClass() throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "JDK 21 compiler is part of the documented acceptance toolchain");
        Path api = Files.createDirectories(directory.resolve("old-api/atf/reporting/model"));
        Path stub = api.resolve("ReportTagNormalizer.java");
        Files.writeString(stub, """
                package atf.reporting.model;
                public final class ReportTagNormalizer {
                    private ReportTagNormalizer() { }
                    public static java.util.List<String> normalize(java.util.Collection<String> input) {
                        throw new AssertionError("The ABI stub must never be used at runtime");
                    }
                }
                """);
        Path apiClasses = Files.createDirectories(directory.resolve("old-api-classes"));
        assertEquals(0, compiler.run(null, null, null, "-proc:none", "--release", "21", "-d", apiClasses.toString(), stub.toString()));
        Path caller = directory.resolve("LegacyTagCaller.java");
        Files.writeString(caller, """
                public final class LegacyTagCaller {
                    public static java.util.List<String> invoke() {
                        return atf.reporting.model.ReportTagNormalizer.normalize(
                            java.util.Arrays.asList(" API ", null, "UI", "api", ""));
                    }
                }
                """);
        Path callerClasses = Files.createDirectories(directory.resolve("caller-classes"));
        assertEquals(0, compiler.run(null, null, null, "-proc:none", "--release", "21", "-classpath", apiClasses.toString(),
                "-d", callerClasses.toString(), caller.toString()));
        try (var loader = new URLClassLoader(new java.net.URL[]{callerClasses.toUri().toURL()}, ReportTagNormalizer.class.getClassLoader())) {
            assertSame(ReportTagNormalizer.class, loader.loadClass("atf.reporting.model.ReportTagNormalizer"));
            assertEquals(List.of("api", "ui"), loader.loadClass("LegacyTagCaller").getMethod("invoke").invoke(null));
        }
    }
}
