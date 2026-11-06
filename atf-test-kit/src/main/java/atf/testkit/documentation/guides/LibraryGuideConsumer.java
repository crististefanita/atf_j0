package atf.testkit.documentation.guides;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Reusable test harness; behavior oracles stay with each library's native test owner. */
public final class LibraryGuideConsumer {
    private final Path work;
    private final GuideMarkdown guide;
    private final List<Path> classpath;
    private final GuideBundle.Artifact artifact;

    public LibraryGuideConsumer(Path work, String library) throws Exception {
        this.work = work.toAbsolutePath().normalize();
        Files.createDirectories(this.work);
        Path bundle = GuideBundle.selected();
        var artifacts = GuideBundle.publish(bundle, work.resolve("published"));
        artifact = artifacts.stream().filter(item -> item.id().equals(library.equals("TestKit") ? "atf-test-kit" : "atf-" + library.toLowerCase(java.util.Locale.ROOT))).findFirst().orElseThrow();
        guide = new GuideMarkdown(Files.readString(bundle.resolve("README." + library + ".md")));
        Files.writeString(work.resolve("pom.xml"), pom(artifact, work.resolve("published")));
        var resolution = GuideProcesses.maven(work, List.of("org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath",
                "-DincludeScope=runtime", "-Dmdep.outputFile=" + work.resolve("runtime.cp")));
        if (resolution.exit() != 0) throw new IllegalStateException("Guide dependencies could not resolve: " + resolution.log());
        classpath = Pattern.compile(Pattern.quote(File.pathSeparator)).splitAsStream(Files.readString(work.resolve("runtime.cp")).strip())
                .map(Path::of).map(Path::toAbsolutePath).toList();
        validateClasspath(artifacts);
    }

    public GuideMarkdown guide() { return guide; }
    public List<Path> classpath() { return classpath; }
    public Path directory() { return work; }

    public GuideCompilation compile(String label, boolean method, String oracle) throws Exception {
        return compile(label, guide.executable(method, oracle, binaryOracle()), classpath);
    }

    public GuideCompilation compile(String label, String source, List<Path> dependencies) throws Exception {
        for (Path jar : dependencies) if (!Files.isRegularFile(jar) || !jar.toString().endsWith(".jar"))
            throw new IllegalArgumentException("Guide compiler accepts only physical dependency JARs: " + jar);
        return GuideCompilation.compile(work.resolve(label), source, dependencies);
    }

    public String execute(GuideCompilation compiled) throws Exception {
        if (!compiled.passed()) throw new IllegalStateException("Cannot execute an uncompiled guide: " + compiled.diagnostics());
        Path temporary = Files.createDirectories(work.resolve("jvm-tmp"));
        var command = new ArrayList<String>(List.of(Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString(),
                "-Xmx256m", "-Djava.io.tmpdir=" + temporary, "-cp", compiled.classes() + File.pathSeparator
                + String.join(File.pathSeparator, classpath.stream().map(Path::toString).toList()), "GuideExample", work.resolve("reports").toString()));
        var result = GuideProcesses.run(work, "execution", command);
        if (result.exit() != 0) throw new AssertionError("Guide execution failed: " + result.log() + "\n" + result.text());
        if (!result.text().contains("GUIDE_BINARY=" + artifact.id() + "; SHA256=" + artifact.hash()))
            throw new AssertionError("Guide did not prove the selected physical JAR");
        return result.text();
    }

    public void verifyRefusals(boolean method, String oracle, String call) throws Exception {
        String source = guide.executable(method, oracle, binaryOracle());
        if (!source.contains(call)) throw new IllegalStateException("The negative control no longer targets a documented API call: " + call);
        var retired = compile("retired-api", source.replaceFirst(Pattern.quote(call), ".withdrawnGuideMethod("), classpath);
        if (retired.passed() || !retired.diagnostics().contains("withdrawnGuideMethod"))
            throw new AssertionError("A retired API must fail guide compilation: " + retired.diagnostics());
        var missing = classpath.stream().filter(file -> !file.getFileName().toString().startsWith(artifact.id() + "-")).toList();
        var refusal = compile("missing-library", source, missing);
        if (refusal.passed()) throw new AssertionError("The guide compiled without its declared library");
    }

    private void validateClasspath(List<GuideBundle.Artifact> artifacts) throws Exception {
        boolean found = false;
        for (Path jar : classpath) {
            if (!Files.isRegularFile(jar) || !jar.toString().endsWith(".jar")) throw new IllegalStateException("Source shadow in guide classpath: " + jar);
            String name = jar.getFileName().toString();
            if (name.startsWith("atf-") && !name.startsWith(artifact.id() + "-")
                    && !(List.of("atf-mock", "atf-console").contains(artifact.id()) && name.startsWith("atf-core-")))
                throw new IllegalStateException("Unexpected ATF dependency: " + jar);
            if (name.startsWith("atf-")) {
                var published = artifacts.stream().filter(item -> item.jar().getFileName().toString().equals(name)).findFirst()
                        .orElseThrow(() -> new IllegalStateException("ATF dependency is not in the selected bundle: " + name));
                if (!GuideBundle.hash(jar).equals(published.hash())) throw new IllegalStateException("Resolved dependency differs from bundle: " + name);
            }
            if (name.equals(artifact.jar().getFileName().toString())) {
                if (!GuideBundle.hash(jar).equals(artifact.hash())) throw new IllegalStateException("Resolved JAR differs from bundle");
                found = true;
            }
        }
        if (!found) throw new IllegalStateException("The published library is absent from the consumer classpath");
    }

    private String binaryOracle() {
        String entry = switch (artifact.id()) {
            case "atf-test-kit" -> "atf.testkit.assertions.RecordedAssertions";
            case "atf-core" -> "atf.core.http.request.ApiRequest";
            case "atf-console" -> "atf.console.AtfConsoleReporter";
            case "atf-mock" -> "atf.mock.AtfMockTransport";
            case "atf-reporting" -> "atf.reporting.extent.session.ExtentReportSession";
            default -> throw new IllegalStateException("Unknown guide library");
        };
        String isolation = artifact.id().equals("atf-test-kit")
                ? "try { Class.forName(\"atf.reporting.audit.AssertionAuditRun\"); throw new AssertionError(\"Reporting leaked into Kit consumer\"); } catch (ClassNotFoundException expected) { }"
                : "try { Class.forName(\"atf.testkit.AtfUnitTest\"); throw new AssertionError(\"Kit leaked into consumer\"); } catch (ClassNotFoundException expected) { }";
        return """
                private static void assertBinary() throws Exception {
                    var loaded = java.nio.file.Path.of(Class.forName("%s").getProtectionDomain().getCodeSource().getLocation().toURI());
                    if (!java.nio.file.Files.isRegularFile(loaded) || !loaded.toString().endsWith(".jar")) throw new AssertionError("Source shadow: " + loaded);
                    var digest = java.security.MessageDigest.getInstance("SHA-256");
                    String hash = java.util.HexFormat.of().formatHex(digest.digest(java.nio.file.Files.readAllBytes(loaded)));
                    if (!hash.equals("%s")) throw new AssertionError("Wrong binary hash");
                    %s
                    System.out.println("GUIDE_BINARY=%s; SHA256=" + hash);
                }
                """.formatted(entry, artifact.hash(), isolation, artifact.id());
    }

    private static String pom(GuideBundle.Artifact artifact, Path published) {
        String cache = Path.of(System.getProperty("user.home"), ".m2/repository").toUri().toString();
        return """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion><groupId>guide.consumer</groupId><artifactId>standalone</artifactId><version>1</version>
                  <repositories><repository><id>published-libraries</id><url>%s</url></repository>
                    <repository><id>cached-vendors</id><url>%s</url></repository></repositories>
                  <pluginRepositories><pluginRepository><id>cached-plugins</id><url>%s</url></pluginRepository></pluginRepositories>
                  <dependencies><dependency><groupId>atf.api</groupId><artifactId>%s</artifactId><version>%s</version></dependency></dependencies>
                </project>
                """.formatted(published.toUri(), cache, cache, artifact.id(), artifact.version());
    }
}
