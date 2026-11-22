<a id="test-kit--junit-classification-and-audit"></a>
<a id="test-kit--clasificare-și-audit-junit"></a>
# Test Kit — JUnit classification and audit

<a id="at-a-glance"></a>
<a id="pe-scurt"></a>
## At a glance

Kit provides annotations, evaluated comparisons, and a neutral lifecycle. The JAR does not start a browser, broker, or ADB, and does not publish Extent on its own.

| Need | API and limit |
|---|---|
| Test level | @AtfUnitTest, @AtfComponentTest, @AtfIntegrationTest, @AtfE2eTest; classification only, with no automatic launch. |
| Auditable comparisons | RecordedAssertions; the JUnit verdict precedes the 240-character preview, which preserves complete Unicode pairs. |
| Existing JUnit assertions | AuditedAssertions within the adapter scope; lazy diagnostics, original identity and cause. Direct JUnit calls do not become auditable automatically. |
| Full values | AssertionValueDisplay can receive a Projection observer with the expected/actual role and redacted text before preview; the consumer owns retention. |
| Phases and cleanup | ComponentAuditExtension / ComponentAuditAdapter, TestRunInitialization, FailureAwareCleanup. |
| Policies and resources | Atf*Policy resolvers; readResourceUtf8 for classpath/JAR, requiredPath for a real file. |

<a id="minimal-example"></a>
<a id="exemplu-minim"></a>
## Minimal example

The adapter owns the recorder and publishes the result; a mismatch preserves the original JUnit exception:

```java
import atf.testkit.assertions.RecordedAssertions;

public static void verifyState(RecordedAssertions audit, String actual) {
    audit.equals("State", "ready", actual);
}
```

<a id="scope-and-integration"></a>
<a id="scope-și-integrare"></a>
## Scope and integration

- Dependency: atf.api:atf-test-kit with the atf.testkit.version from the project POM. Use test scope in production; compile is justified only for main-scope support in a test module. Do not include Kit in an APK/application.
- JUnit Jupiter API and Jackson are transitive. The runner declares the junit-jupiter-engine and its selection; the level annotation does not replace @Test or the consumer extension.
- For Extent: the adapter sends checks to AssertionAuditReport; AssertionAuditRun owns the shared session. Kit does not copy the renderer. [Reporting integration](README.Reporting.md#scope-and-integration).
- Inject the sanitizer through AssertionValueDisplay before preview. The comparison runs once; stored values are bounded and are not a complete archive. Pure units keep their own policy.
- Scopes and resources are per invocation; close them and preserve the original cause. A local lock does not isolate global policies from external readers.

---

Maintenance, code, tests and feedback: [atf_j0](README.md#mentenanță-și-feedback).

<a id="real-junit-launch-plan"></a>
<a id="planul-real-al-lansării-junit"></a>
## Real JUnit launch plan

The `atf.testkit.launch` package observes launcher sessions through the public `LauncherSessionListener` SPI. It neither executes tests nor opens consumer resources. `TestLaunchContext.capture(context)` returns an immutable `Optional<TestLaunchSnapshot>` for the current invocation: request selectors, filtered `TestPlan` inventory, initially discovered count and dynamic registrations observed at capture time.

- Request association uses plan configuration identity; a foreign or unavailable
  request does not replace provenance of the executed plan.
- Suites pass outer context only to actual descendants. Independent, repeated
  and concurrent launches retain their own scope and cleanup.
- The discovered count includes disabled tests and excludes later dynamic
  registrations; it does not represent the number of passed or executed tests.
- Capture is absent when the SPI observer is disabled. For a plan discovered
  in another session, inventory and count remain available; selector provenance explicitly reports the unavailable request.
- `junit-platform-launcher` is optional; the consumer launching tests provides
  it. The library has no dependency on Reporting, UI or consumers.

`TestLaunchPlanComponentTest` probes direct/filtered selection, nested/concurrent scopes, plans executed later, disabled tests, dynamic registrations and absence of the bridge.

[Consumer integration guides](README.md#consumer-guides).
