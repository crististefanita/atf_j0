package atf.core.config.module;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Immutable module selection used by test composition roots. */
public record AtfModuleSet(Set<AtfModule> modules, boolean explicitlyConfigured) {
    private static final Set<AtfModule> ALL_MODULES = Set.of(AtfModule.values());

    public AtfModuleSet {
        modules = Set.copyOf(Objects.requireNonNull(modules, "modules"));
    }

    public boolean contains(AtfModule module) {
        return modules.contains(Objects.requireNonNull(module, "module"));
    }

    public boolean hasUi() {
        return contains(AtfModule.UI);
    }

    public boolean hasReporting() {
        return contains(AtfModule.REPORTING);
    }

    public static AtfModuleSet all() {
        return new AtfModuleSet(ALL_MODULES, false);
    }

    public static AtfModuleSet explicit(Iterable<AtfModule> modules) {
        EnumSet<AtfModule> selected = EnumSet.noneOf(AtfModule.class);
        modules.forEach(selected::add);
        return new AtfModuleSet(selected, true);
    }
}
