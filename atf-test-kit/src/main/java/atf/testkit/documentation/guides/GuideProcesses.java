package atf.testkit.documentation.guides;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Bounded child processes; no consumer command inherits the host reactor classpath. */
final class GuideProcesses {
    private GuideProcesses() { }

    static Result maven(Path work, List<String> arguments) throws Exception {
        var command = new ArrayList<String>();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        if (windows) command.addAll(List.of("cmd.exe", "/d", "/c"));
        command.add(System.getProperty("atf.guide.maven", "mvn"));
        command.addAll(List.of("-B", "-ntp", "-Dmaven.repo.local=" + work.resolve("m2")));
        String seed = System.getProperty("atf.guide.maven.seed", "");
        if (!seed.isBlank()) {
            GuideMavenCache.prepare(work, Path.of(seed));
            command.add("--offline");
        }
        String settings = System.getProperty("atf.guide.maven.settings", "");
        if (!settings.isBlank()) command.addAll(List.of("-s", Path.of(settings).toAbsolutePath().toString()));
        command.addAll(arguments);
        if (windows && command.stream().anyMatch(value -> value.matches("(?s).*[\"&|<>^%\\r\\n].*")))
            throw new IllegalArgumentException("Unsupported shell metacharacter in Maven arguments");
        return run(work, "maven", command);
    }

    static Result run(Path work, String label, List<String> command) throws Exception {
        Path log = work.resolve(label + ".log");
        var builder = new ProcessBuilder(command).directory(work.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().remove("CLASSPATH");
        builder.environment().remove("ATF_REPORT_CONFIG");
        builder.environment().remove("ATF_MOCK_CONFIG");
        String options = builder.environment().getOrDefault("JAVA_TOOL_OPTIONS", "");
        builder.environment().put("JAVA_TOOL_OPTIONS", options + " -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8");
        var process = builder.start();
        try {
            if (!process.waitFor(180, TimeUnit.SECONDS)) throw new IllegalStateException("Guide process exceeded 180s: " + log);
        } finally {
            if (process.isAlive()) { process.descendants().forEach(ProcessHandle::destroyForcibly); process.destroyForcibly(); }
        }
        return new Result(process.exitValue(), Files.readString(log), log);
    }

    record Result(int exit, String text, Path log) { }
}
