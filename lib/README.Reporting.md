# Reporting — dovezi și rapoarte auditabile

## Pe scurt

`atf-reporting` publică observații, aserțiuni și capturi în Extent sau HTML fallback. Consumerul furnizează datele originale, comparația și verdictul; biblioteca deține proiecția și publicarea artifactului.

| Tip de dovadă | API de pornire |
|---|---|
| Raport Extent | `ExtentReportSession` și porturile `AtfReportPorts` |
| Aserțiuni de componentă, fără request artificial | `Phase.startAssertionStep(...)`, `AtfAssertion` |
| Browser sau Android, cu capturi furnizate de adaptor | `UiExtentReportSession`, `UiExtentReportOptions` |
| Publicare HTML fallback | `HtmlReportSession`, cu lifecycle propriu |

## Exemplu minim

Metodă Java pentru o rulare individuală; `actual` poate fi `null`. În suite utilizați sesiunea comună a adaptorului local:

```java
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import java.nio.file.Path;
import java.util.List;

public static void publish(Path output, String actual) {
    String expected = "ready";
    boolean passed = expected.equals(actual);
    var status = passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
    try (var report = new ExtentReportSession(output, ReportingSettings.defaults());
         var scenario = report.startScenario("HealthContract", "state is ready", List.of("component"));
         var phase = scenario.startPhase("TEST BODY");
         var checks = phase.startAssertionStep("Component assertions")) {
        checks.assertions(List.of(new AtfAssertion("State", expected, String.valueOf(actual), passed)));
        checks.complete(status);
        phase.complete(status);
        scenario.complete(status);
    }
    if (!passed) throw new AssertionError("State differs; inspect the report");
}
```

## Integrare corectă

- Deschideți sesiunea la ownerul rulării sau suitei; un fișier comun cere o sesiune partajată și scope-uri distincte pentru scenarii.
- Închideți copilul înaintea părintelui și sesiunea la final; artifactul Extent final este disponibil după `close()`.
- Trimiteți valorile integrale înainte de preview; limita implicită pentru afișarea aserțiunilor este **240 de caractere**.
- Furnizați PNG-ul întregului viewport sau display; zoom-ul, capturarea și oracolul sunt responsabilitatea adaptorului.
- Publicați timing-ul măsurat de caller separat de costul publicării; înregistrați explicit verdictul fiecărui scope.

Reporting este thin și folosește modelele `atf.reporting.*`, distincte de Core. Consumerul citește YAML-ul și transmite `ReportingSettings`; `defaults()` folosește valorile JAR-ului.

---

Mentenanța codului și regresiilor generice aparține proiectelor sursă `atf_j0/atf-reporting` și `atf_j0/atf-reporting-tests`; [feedback-ul și sincronizarea](README.md#mentenanță-și-feedback) pornesc de la J0.
