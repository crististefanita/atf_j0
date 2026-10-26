package atf.core.reporting.duration;

import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses every duration form accepted by the Extent runtime. */
public final class AtfDurationParser {
    private static final Pattern LONG = Pattern.compile("^(\\d{2}):(\\d{2}):(\\d{2}):(\\d{3})(?:\\s*ms)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHORT = Pattern.compile("^(\\d{2}):(\\d{2}):(\\d{3})(?:\\s*ms)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MILLIS = Pattern.compile("^(\\d+(?:\\.\\d+)?)\\s*ms$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECONDS = Pattern.compile("^(\\d+(?:\\.\\d+)?)\\s*s$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAIN = Pattern.compile("^\\d+(?:\\.\\d+)?$");

    private AtfDurationParser() { }

    public static OptionalLong parse(String raw) {
        String value = raw == null ? "" : raw.trim();
        Matcher matcher = LONG.matcher(value);
        if (matcher.matches()) return OptionalLong.of(Long.parseLong(matcher.group(1)) * 3_600_000L
                + Long.parseLong(matcher.group(2)) * 60_000L + Long.parseLong(matcher.group(3)) * 1_000L
                + Long.parseLong(matcher.group(4)));
        matcher = SHORT.matcher(value);
        if (matcher.matches()) return OptionalLong.of(Long.parseLong(matcher.group(1)) * 60_000L
                + Long.parseLong(matcher.group(2)) * 1_000L + Long.parseLong(matcher.group(3)));
        matcher = MILLIS.matcher(value);
        if (matcher.matches()) return OptionalLong.of((long) Double.parseDouble(matcher.group(1)));
        matcher = SECONDS.matcher(value);
        if (matcher.matches()) return OptionalLong.of((long) (Double.parseDouble(matcher.group(1)) * 1_000));
        if (PLAIN.matcher(value).matches()) return OptionalLong.of((long) Double.parseDouble(value));
        return OptionalLong.empty();
    }
}
