package atf.reporting.browser.lifecycle;

import java.nio.file.Files;
import atf.reporting.browser.runtime.BrowserDriverRuntime;
import atf.reporting.browser.process.BrowserProcessInventory;
import atf.reporting.browser.process.BrowserPhaseDeadline;
import atf.reporting.browser.process.BrowserPhaseChannel;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** A stuck SDK call is contained by process ownership rather than an interrupt on a shared browser thread. */
public final class BrowserProbeProcess {
    private BrowserProbeProcess() { }

    public static Path run(Path root, Class<? extends BrowserProbe> factory, String... arguments) throws Exception {
        return run(root, factory, BrowserProbeBudget.DEFAULT, arguments);
    }

    public static Path run(Path root, Class<? extends BrowserProbe> factory, BrowserProbeBudget budget, String... arguments) throws Exception {
        return run(root, factory, budget, List.of(), arguments);
    }

    public static Path run(Path root, Class<? extends BrowserProbe> factory, BrowserProbeBudget budget, List<Path> classpath, String... arguments) throws Exception {
        Files.createDirectories(root);
        Path directory = Files.createTempDirectory(root, "browser-probe-").toAbsolutePath();
        BrowserDriverRuntime.Installation driver;
        try {
            driver = BrowserDriverRuntime.prepare();
            Files.writeString(directory.resolve("driver-runtime.log"),
                    "source=" + driver.source() + "\npreparationMillis=" + driver.preparationMillis()
                            + "\ncliDirectory=" + driver.directory());
        } catch (Exception error) {
            Files.writeString(directory.resolve("driver-runtime.log"), "preparation=FAILED\n" + error);
            throw error;
        }
        String temporary = System.getenv("TEMP");
        Path scratch = Files.createTempDirectory(Path.of(temporary == null ? System.getProperty("java.io.tmpdir") : temporary), "atf-bp-").toAbsolutePath();
        Files.writeString(scratch.resolve("ATF_OWNER"), directory.toString());
        Files.writeString(directory.resolve("scratch.path"), scratch.toString());
        var command = new ArrayList<String>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-Xmx256m");
        command.add("-Dfile.encoding=UTF-8");
        command.add("-Dstdout.encoding=UTF-8");
        command.add("-Dstderr.encoding=UTF-8");
        command.add("-Djava.io.tmpdir=" + scratch);
        for (String key : System.getProperties().stringPropertyNames().stream().filter(value -> value.startsWith("atf.")).sorted().toList()) {
            String value = key.equals("atf.test.reportDirectory") ? scratch.resolve("reports").toString() : System.getProperty(key);
            command.add("-D" + key + "=" + value);
        }
        if (System.getProperty("atf.test.reportDirectory") == null) command.add("-Datf.test.reportDirectory=" + scratch.resolve("reports"));
        command.add("-Dplaywright.cli.dir=" + driver.directory());
        String extra = classpath.stream().map(Path::toString).collect(java.util.stream.Collectors.joining(java.io.File.pathSeparator));
        String path = System.getProperty("java.class.path") + (extra.isEmpty() ? "" : java.io.File.pathSeparator + extra);
        command.addAll(List.of("-cp", path, BrowserProbeWorker.class.getName(),
                directory.toString(), factory.getName()));
        command.addAll(List.of(arguments));
        Path argfile = directory.resolve("java.args");
        Files.write(argfile, command.subList(1, command.size()).stream().map(BrowserProbeProcess::quote).toList());
        var builder = new ProcessBuilder(command.get(0), "@" + argfile).redirectErrorStream(true)
                .redirectOutput(directory.resolve("process.log").toFile());
        for (String key : List.of("TEMP", "TMP", "TMPDIR")) builder.environment().put(key, scratch.toString());
        // Browser binaries are provisioned before the probe; installation is not measured test setup.
        builder.environment().put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1");
        Process process = builder.start();
        var owned = new BrowserProcessInventory(process.toHandle());
        String phase = "SETUP";
        var deadline = new BrowserPhaseDeadline();
        long lifetime = System.nanoTime();
        long maximum = budget.lifetime().toNanos();
        Throwable primary = null;
        try (var phases = new BrowserPhaseChannel(directory)) {
            while (process.isAlive()) {
                String latest = phases.advanceTimed(frame -> deadline.observe(frame, budget));
                phase = deadline.phase();
                if (deadline.expired(budget) || System.nanoTime() - lifetime >= maximum) {
                    Files.writeString(directory.resolve("diagnose"), phase + "\nevent=" + latest + "\nelapsedNanos=" + deadline.elapsedNanos());
                    long diagnosticStart = System.nanoTime();
                    while (process.isAlive() && !Files.exists(directory.resolve("threads.log"))
                            && System.nanoTime() - diagnosticStart < Duration.ofMillis(500).toNanos()) Thread.sleep(10);
                    // A committed boundary can race the watchdog. Only worker-timed progress within its prior budget can advance it.
                    latest = phases.advanceTimed(frame -> deadline.observe(frame, budget));
                    phase = deadline.phase();
                    if (deadline.expired(budget) || System.nanoTime() - lifetime >= maximum)
                        throw failure(directory, process, owned, phase, "Browser probe exceeded its own " + budget.forPhase(phase) + " deadline; event=" + latest);
                    Files.move(directory.resolve("diagnose"), directory.resolve("diagnose-reconciled-" + latest));
                }
                owned.captureIfDue();
                process.waitFor(10, TimeUnit.MILLISECONDS);
            }
            String lastEvent = phases.advanceTimed(frame -> deadline.observe(frame, budget));
            phase = deadline.phase();
            if (deadline.expired(budget) || System.nanoTime() - lifetime >= maximum)
                throw failure(directory, process, owned, phase, "Browser probe exceeded its own " + budget.forPhase(phase) + " deadline; event=" + lastEvent);
            String terminal = lastEvent.substring(15);
            if (process.exitValue() != 0 || !terminal.equals("DONE"))
                throw failure(directory, process, owned, read(directory.resolve("failure-phase")).isBlank() ? phase
                        : read(directory.resolve("failure-phase")), "Browser probe did not complete successfully; exit=" + process.exitValue());
            long cleanupStarted = phase.equals("CLEANUP") ? deadline.startedAt() : System.nanoTime();
            while (!owned.aliveHandles().isEmpty()
                    && System.nanoTime() - cleanupStarted < budget.cleanup().toNanos()) Thread.sleep(10);
            if (!owned.aliveHandles().isEmpty())
                throw failure(directory, process, owned, "CLEANUP", "Browser probe left owned resources alive");
            Files.writeString(directory.resolve("outcome"), "PASS\npid=" + process.pid());
            return directory;
        } catch (Exception | Error error) {
            primary = error;
            if (error instanceof InterruptedException) Thread.currentThread().interrupt();
            try {
                if (!Files.exists(directory.resolve("outcome")))
                    Files.writeString(directory.resolve("outcome"), "FAIL\nphase=" + phase + "\npid=" + process.pid() + "\n" + error);
            } catch (Exception diagnostic) { error.addSuppressed(diagnostic); }
            throw error;
        } finally {
            owned.captureNow();
            try { terminate(owned, Duration.ofSeconds(5)); }
            catch (Exception error) { if (primary == null) throw error; else primary.addSuppressed(error); }
        }
    }

    private static BrowserProbeFailure failure(Path directory, Process process, BrowserProcessInventory owned, String phase, String reason) throws Exception {
        owned.captureNow();
        var inventory = new StringBuilder(reason).append("\nphase=").append(phase).append('\n');
        owned.handles().values().forEach(handle -> inventory.append("pid=").append(handle.pid()).append(" alive=")
                .append(owned.isSameLiveProcess(handle)).append(" ").append(handle.info()).append('\n'));
        Files.writeString(directory.resolve("processes.log"), inventory);
        Files.writeString(directory.resolve("outcome"), "FAIL\n" + inventory);
        return new BrowserProbeFailure(phase, process.pid(), directory, reason,
                read(directory.resolve("failure.log")) + read(directory.resolve("process.log")));
    }

    private static void terminate(BrowserProcessInventory owned, Duration budget) throws Exception {
        var handles = new ArrayList<>(owned.aliveHandles().values());
        handles.forEach(handle -> {
            if (owned.isSameLiveProcess(handle)) handle.destroyForcibly();
        });
        long started = System.nanoTime();
        boolean interrupted = Thread.interrupted();
        try {
            while (handles.stream().anyMatch(owned::isSameLiveProcess)
                    && System.nanoTime() - started < budget.toNanos()) {
                try { Thread.sleep(10); } catch (InterruptedException error) { interrupted = true; }
            }
        } finally { if (interrupted) Thread.currentThread().interrupt(); }
        if (handles.stream().anyMatch(owned::isSameLiveProcess))
            throw new IllegalStateException("Owned browser probe resources did not terminate: " + handles.stream()
                    .filter(owned::isSameLiveProcess).map(ProcessHandle::pid).toList());
    }

    private static String read(Path file) throws Exception { return Files.exists(file) ? new String(Files.readAllBytes(file), java.nio.charset.StandardCharsets.UTF_8) : ""; }
    private static String quote(String value) { return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }
}
