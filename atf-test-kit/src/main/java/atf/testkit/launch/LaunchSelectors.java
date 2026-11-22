package atf.testkit.launch;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.discovery.ClasspathRootSelector;

import java.util.List;

/** Stable source selectors; never serializes arbitrary selector toString or display data. */
final class LaunchSelectors {
    private LaunchSelectors() { }
    static List<String> describe(List<DiscoverySelector> selectors) {
        return selectors.stream().map(LaunchSelectors::describe).sorted().toList();
    }
    private static String describe(DiscoverySelector selector) {
        if (selector instanceof ClassSelector value) return "class: " + value.getClassName();
        if (selector instanceof MethodSelector value) return "method: " + value.getClassName() + "#"
                + value.getMethodName() + "(" + value.getMethodParameterTypes() + ")";
        if (selector instanceof PackageSelector value) return "package: " + value.getPackageName();
        if (selector instanceof UniqueIdSelector value) return "unique-id: " + value.getUniqueId();
        if (selector instanceof ClasspathRootSelector value) return "classpath-root: " + value.getClasspathRoot();
        return "selector-type: " + selector.getClass().getName() + " (value unavailable)";
    }
}
