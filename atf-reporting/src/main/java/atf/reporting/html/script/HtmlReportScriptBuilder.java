package atf.reporting.html.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Deterministic composition model for named report JavaScript fragments. */
public final class HtmlReportScriptBuilder {
    private final List<Fragment> fragments = new ArrayList<>();

    public HtmlReportScriptBuilder append(String owner, String source) {
        fragments.add(new Fragment(owner, source));
        return this;
    }

    public HtmlReportScriptBuilder append(HtmlReportScriptModule module, String source) {
        return append(Objects.requireNonNull(module, "module").owner(), source);
    }

    public List<Fragment> fragments() {
        return List.copyOf(fragments);
    }

    public String build() {
        return fragments.stream().map(Fragment::source).collect(Collectors.joining());
    }

    public record Fragment(String owner, String source) {
        public Fragment {
            if (Objects.requireNonNull(owner, "owner").isBlank()) {
                throw new IllegalArgumentException("owner must not be blank");
            }
            source = Objects.requireNonNull(source, "source");
        }
    }
}
