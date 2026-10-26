package atf.debugui;

/**
 * Prevents an accidentally unbounded Debug UI JVM from competing with
 * parallel report audits for native memory.
 */
final class DebugUiMemoryPolicy {
    static final long DEFAULT_MAX_HEAP_BYTES = 512L * 1024L * 1024L;

    private DebugUiMemoryPolicy() {
    }

    static void enforce() {
        long configuredLimitMb = Long.getLong("atf.debugui.maxHeapMb", 512L);
        if (configuredLimitMb <= 0L) {
            throw new IllegalStateException("atf.debugui.maxHeapMb must be positive");
        }
        long configuredLimit = Math.multiplyExact(configuredLimitMb, 1024L * 1024L);
        enforce(Runtime.getRuntime().maxMemory(), configuredLimit);
    }

    static void enforce(long maxHeapBytes, long allowedHeapBytes) {
        if (maxHeapBytes <= allowedHeapBytes) {
            return;
        }
        long actualMb = maxHeapBytes / (1024L * 1024L);
        long allowedMb = allowedHeapBytes / (1024L * 1024L);
        throw new IllegalStateException("ATF Debug UI refuses an unbounded JVM heap: max="
                + actualMb + " MB, limit=" + allowedMb
                + " MB. Launch with -Xms16m -Xmx128m -XX:MaxMetaspaceSize=64m "
                + "or set -Datf.debugui.maxHeapMb=<positive MB>. ");
    }
}
