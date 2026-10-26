package atf.reporting.api.tests.support.setup;
import atf.core.testing.api.ApiScenarioSelection;
import atf.testkit.setup.api.ApiTestRef;

import atf.core.reporting.contract.AtfReportPorts;
import atf.reporting.api.execution.ApiModule;
import atf.core.lifecycle.ApiScenarioLifecycleRunner;
import atf.reporting.api.tests.api.headers.ApiHeadersAndEdgeCaseScenarios;
import atf.reporting.api.tests.api.lifecycle.LifecycleApiScenarios;
import atf.reporting.api.tests.api.flows.ApiRequestFlowScenarios;
import atf.reporting.api.tests.api.negative.ApiNegativeScenarios;
import atf.reporting.api.tests.api.posts.PostsApiScenarios;
import atf.reporting.api.tests.api.requests.RequestVariantsApiScenarios;
import atf.reporting.api.tests.api.users.UsersApiScenarios;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Ordered composition root for the API artifact consumed by UI tests. */
public final class ApiScenarioSetupCatalog {
    public static final List<String> DEFAULT_ORDER = List.of(
            "posts", "users", "requests", "headers", "flows", "negative", "lifecycle");

    private ApiScenarioSetupCatalog() { }

    public static void publish(AtfReportPorts.Session session, ApiModule api,
                               ApiScenarioLifecycleRunner lifecycle,
                               Collection<String> requestedFamilies) {
        Collection<String> families = requestedFamilies == null || requestedFamilies.isEmpty()
                ? DEFAULT_ORDER : requestedFamilies;
        for (String family : families) publishOne(session, api, lifecycle, family);
    }

    /** Publishes only the exact API scenario methods declared by an UI audit. */
    public static void publishSelected(AtfReportPorts.Session session, ApiModule api,
                                       ApiScenarioLifecycleRunner lifecycle,
                                       Collection<ApiScenarioSelection> selections) {
        Objects.requireNonNull(selections, "selections");
        if (selections.isEmpty()) {
            throw new IllegalArgumentException("At least one API scenario must be selected");
        }
        for (ApiScenarioSelection selection : selections) {
            publishOne(session, api, lifecycle, selection);
        }
    }

    /** Resolves annotation metadata into a renderer-neutral, report-aware selection. */
    public static ApiScenarioSelection resolve(ApiTestRef reference) {
        Objects.requireNonNull(reference, "reference");
        Class<?> owner = reference.owner();
        String method = reference.method();
        if (owner == UsersApiScenarios.class) {
            UsersApiScenarios.scenarioTitle(method);
            return new ApiScenarioSelection(owner, method, "users",
                    UsersApiScenarios.REPORT_OWNER, UsersApiScenarios.scenarioTitle(method),
                    List.of(UsersApiScenarios.scenarioTitle(method)),
                    List.of(UsersApiScenarios.expectedStatus(method)));
        }
        if (owner == PostsApiScenarios.class) {
            PostsApiScenarios.scenarioTitle(method);
            return new ApiScenarioSelection(owner, method, "posts",
                    PostsApiScenarios.REPORT_OWNER, PostsApiScenarios.scenarioTitle(method));
        }
        if (owner == RequestVariantsApiScenarios.class) {
            return new ApiScenarioSelection(owner, method, "requests",
                    RequestVariantsApiScenarios.REPORT_OWNER,
                    RequestVariantsApiScenarios.scenarioTitle(method));
        }
        if (owner == ApiHeadersAndEdgeCaseScenarios.class) {
            return new ApiScenarioSelection(owner, method, "headers",
                    ApiHeadersAndEdgeCaseScenarios.REPORT_OWNER,
                    ApiHeadersAndEdgeCaseScenarios.scenarioTitle(method));
        }
        if (owner == ApiRequestFlowScenarios.class) {
            return new ApiScenarioSelection(owner, method, "flows",
                    ApiRequestFlowScenarios.REPORT_OWNER,
                    ApiRequestFlowScenarios.scenarioTitle(method));
        }
        if (owner == ApiNegativeScenarios.class) {
            return new ApiScenarioSelection(owner, method, "negative",
                    ApiNegativeScenarios.REPORT_OWNER,
                    ApiNegativeScenarios.scenarioTitle(method));
        }
        if (owner == LifecycleApiScenarios.class) {
            return new ApiScenarioSelection(owner, method, "lifecycle",
                    LifecycleApiScenarios.REPORT_OWNER,
                    LifecycleApiScenarios.scenarioTitle(method),
                    LifecycleApiScenarios.SELECTED_REPORT_SCENARIO_TITLES,
                    LifecycleApiScenarios.SELECTED_REPORT_SCENARIO_STATUSES);
        }
        throw new IllegalArgumentException("Unsupported API scenario owner: "
                + owner.getName() + ". Supported owners: "
                + UsersApiScenarios.class.getName() + ", " + PostsApiScenarios.class.getName()
                + ", " + RequestVariantsApiScenarios.class.getName()
                + ", " + ApiHeadersAndEdgeCaseScenarios.class.getName()
                + ", " + ApiRequestFlowScenarios.class.getName()
                + ", " + ApiNegativeScenarios.class.getName()
                + ", " + LifecycleApiScenarios.class.getName());
    }

    public static List<ApiScenarioSelection> resolve(ApiTestRef[] references) {
        Objects.requireNonNull(references, "references");
        List<ApiScenarioSelection> resolved = java.util.Arrays.stream(references)
                .map(ApiScenarioSetupCatalog::resolve).toList();
        Set<String> identities = new LinkedHashSet<>();
        for (ApiScenarioSelection selection : resolved) {
            if (!identities.add(selection.reference())) {
                throw new IllegalArgumentException("Duplicate API scenario reference: "
                        + selection.reference());
            }
        }
        return resolved;
    }

    public static List<String> familiesFor(Collection<ApiScenarioSelection> selections) {
        Objects.requireNonNull(selections, "selections");
        return selections.stream().map(ApiScenarioSelection::family)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new), List::copyOf));
    }

    private static void publishOne(AtfReportPorts.Session session, ApiModule api,
                                   ApiScenarioLifecycleRunner lifecycle, String family) {
        String key = family == null ? "" : family.trim().toLowerCase();
        switch (key) {
            case "posts" -> PostsApiScenarios.publish(session, api, lifecycle);
            case "users" -> UsersApiScenarios.publish(session, api, lifecycle);
            case "requests" -> RequestVariantsApiScenarios.publish(session, api, lifecycle);
            case "headers" -> ApiHeadersAndEdgeCaseScenarios.publish(session, api, lifecycle);
            case "flows" -> ApiRequestFlowScenarios.publish(session, api, lifecycle);
            case "negative" -> ApiNegativeScenarios.publish(session, api, lifecycle);
            case "lifecycle" -> LifecycleApiScenarios.publish(session, api, lifecycle);
            default -> throw new IllegalArgumentException("Unknown API scenario family: " + family
                    + ". Supported families: " + DEFAULT_ORDER);
        }
    }

    private static void publishOne(AtfReportPorts.Session session, ApiModule api,
                                   ApiScenarioLifecycleRunner lifecycle,
                                   ApiScenarioSelection selection) {
        if (selection.ownerType() == UsersApiScenarios.class) {
            UsersApiScenarios.publishSelected(session, api, lifecycle, selection.method());
            return;
        }
        if (selection.ownerType() == PostsApiScenarios.class) {
            PostsApiScenarios.publishSelected(session, api, lifecycle, selection.method());
            return;
        }
        if (selection.ownerType() == RequestVariantsApiScenarios.class) {
            RequestVariantsApiScenarios.publishSelected(session, api, lifecycle, selection.method());
            return;
        }
        if (selection.ownerType() == ApiHeadersAndEdgeCaseScenarios.class) {
            ApiHeadersAndEdgeCaseScenarios.publishSelected(session, api, lifecycle, selection.method());
            return;
        }
        if (selection.ownerType() == ApiRequestFlowScenarios.class) {
            ApiRequestFlowScenarios.publishSelected(session, api, lifecycle, selection.method());
            return;
        }
        if (selection.ownerType() == ApiNegativeScenarios.class) {
            ApiNegativeScenarios.publishSelected(session, api, lifecycle, selection.method());
            return;
        }
        if (selection.ownerType() == LifecycleApiScenarios.class) {
            LifecycleApiScenarios.publishSelected(session, api, lifecycle, selection.method());
            return;
        }
        throw new IllegalArgumentException("Unsupported resolved API scenario owner: "
                + selection.ownerType().getName());
    }
}
