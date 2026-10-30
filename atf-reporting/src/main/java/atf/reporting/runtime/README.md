# ATF Reporting Runtime — interacțiuni în HTML-ul publicat

## Pe scurt

Pachetul compune controllerele JavaScript care rulează în raportul publicat.
`runtime/tests` deține fila Tests din HTML; este cod de producție și nu conține
scenarii JUnit sau teste business ale consumerilor.

[Atlas](../README.md) · [Assets și vocabular](../../../../../../README.md#contracte-și-feedback)

| Comportament | Intrare Java | Sursa efectivă |
|---|---|---|
| ordinea modulelor runtime | [ExtentReportRuntimeModule](core/ExtentReportRuntimeModule.java) | registry typed al ownerilor |
| injecția în document | [ExtentReportRuntimeBehaviour](core/ExtentReportRuntimeBehaviour.java) | runtime boot și scripturi compuse |
| navigare și ancestorii eșecului | [section](section), [failure runtime](core/ExtentReportFailureRuntime.java) | `extent-navigation-*`, `extent-failure-*` |
| Tests/Categories | [tests](tests) | `extent-scenarios`, `extent-tests-filters`, `extent-categories` |
| durate și proiecții | [duration runtime](core/ExtentReportDurationRuntime.java) | `duration-*` |
| vocabular DOM și token-uri | [AtfReportHtmlVocabulary](../markup/vocabulary/AtfReportHtmlVocabulary.java) | selectori și atribute comune markup/runtime |

## De la sursa nativă la artifact

- [ReportAsset](../asset/ReportAsset.java) și manifestul dețin identitatea,
  formatul, path-ul și ordinea inventarului validat.
- [Loaderul](../asset/ReportAssetLoader.java) citește classpath-ul;
  [composerul](../asset/ReportAssetComposer.java) rezolvă token-urile și produce
  copia de publicare. Resursele rămân în [javascript](../../../../resources/atf/reporting/javascript)
  și [css](../../../../resources/atf/reporting/css).
- Facade-urile runtime și [tema](../theme/AtfReportTheme.java) păstrează ordinea
  compoziției. Pipeline-ul rendererului include rezultatul în HTML-ul autonom.

<details>
<summary><strong>Ce se verifică dincolo de existența scriptului</strong></summary>

- Modificarea unui selector cere citirea markup-ului care îl produce și a
  vocabularului Java folosit pentru interpolare.
- Fails trebuie să rezolve identitatea exactă a comparației și ancestorii ei;
  prima validare din TEST BODY nu este un substitut general pentru calea semantică.
- Publicarea navigării amânate depinde de generația curentă. Un callback vechi
  nu trebuie să deschidă scenariul rămas dintr-o selecție anterioară.
- Compactarea este aplicată copiei publicate; sursele native rămân lizibile.
  Prezența tuturor resurselor în directorul sursă nu dovedește conținutul JAR-ului.
- Runtime-ul proiectează durate existente și nu măsoară din nou operația business.

</details>

## Probe și context minim

[ReportAssetContractTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/asset/ReportAssetContractTest.java) și [ReportAssetCompactorTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/asset/ReportAssetCompactorTest.java) verifică resursele;
[ExtentReportRuntimeEvidenceComponentTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/runtime/evidence/ExtentReportRuntimeEvidenceComponentTest.java) verifică runtime-ul compus.
[PublishedFailureNavigationComponentTest](../../../../../../../atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/failurecontext/PublishedFailureNavigationComponentTest.java) verifică vizibilitatea comparației
după backlink, cu un control negativ care ascunde un ancestor.
[PublishedFailureNavigationEdgeComponentTest](../../../../../../../atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/failurecontext/PublishedFailureNavigationEdgeComponentTest.java) completează diagnosticele fără
comparații, fallback-ul și păstrarea rutei/scroll-ului.

Aceste probe sunt în [Reporting Tests](../../../../../../../atf-reporting-tests/README.md).
O schimbare de interacțiune cere și artifact fresh în browser; inspecția textului
JavaScript nu demonstrează click-ul, geometria sau expandarea completă.

