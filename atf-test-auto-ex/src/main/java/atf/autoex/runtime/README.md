# AutoEx runtime — compoziție, observație și finalizare

## Pe scurt

Acest pachet compune browserul, checkpoint-urile și publicarea AutoEx fără
adnotări JUnit. Ownerul este consumerul AutoEx; regula business rămâne la flow/gate,
iar integrarea runnerului rămâne în `src/test`.

## Intrări și responsabilități

| Când investighezi | Citește întâi | Limita |
|---|---|---|
| un checkpoint sau finalizarea run-ului | [AutoExE2eRun](AutoExE2eRun.java) | fațadă peste browser, dovezi și portul de finalizare |
| ownerul contului înainte de checkout | [observarea identității](account/AutoExCustomerIdentityObservation.java) | citește identitatea nevidă; gate-ul compară ownerul |
| ordinea cleanup/publicare/close | [AutoExRunLifecycle](AutoExRunLifecycle.java) | port runtime; implementarea Kit/JUnit se află în test |
| configurație și sesiune de raport | [AutoExReportSession](AutoExReportSession.java) | factory și delegare către biblioteca Reporting |
| download-ul facturii | [AutoExInvoiceDownload](invoice/AutoExInvoiceDownload.java) | un click; recuperare numai pentru GET-ul invoice queue-full observat |
| readiness read-only | [navigarea AutoEx](navigation/AutoExReadOnlyNavigation.java) | politică specifică documentului și originii AutoEx |

## Invariante de păstrat

- Observația nu primește expected-ul clientului și nu autorizează checkout-ul.
  Textul întârziat și documentul înlocuit folosesc deadline-ul inițial.
- `AutoExE2eRun` cere explicit configurația, browserul, faza și lifecycle-ul;
  o extracție runtime nu mută JUnit sau Test Kit în main.
- Browserul, publicarea, timpul și cleanup-ul sunt responsabilități distincte.
  Finalizarea păstrează cauza inițială și eșecurile distincte de cleanup.
- Download-ul este o acțiune măsurată; un click reușit nu o confirmă. Numai
  evenimentul real autorizează citirea fișierului; validatorul verifică factura.
- Queue-full permite numai GET-ul exact al facturii same-origin, în bugetul și
  încercările existente. POST-ul, documentul străin și backoff-ul prea lung sunt refuzate.

## Probe înainte de schimbare

| Contract | Probă de pornire |
|---|---|
| factură și refuzuri | [AutoExInvoiceDownloadComponentTest](../../../../../test/java/atf/autoex/tests/component/pages/AutoExInvoiceDownloadComponentTest.java) |
| identitate și checkout unic | [AutoExCustomerIdentityObservationComponentTest](../../../../../test/java/atf/autoex/tests/component/pages/AutoExCustomerIdentityObservationComponentTest.java) |
| boundary runtime | [AutoExRuntimeBoundaryComponentTest](../../../../../test/java/atf/autoex/tests/component/lifecycle/AutoExRuntimeBoundaryComponentTest.java) |
| ordinea lifecycle-ului | [AutoExRunLifecycleAdapterComponentTest](../../../../../test/java/atf/autoex/tests/component/lifecycle/AutoExRunLifecycleAdapterComponentTest.java) |

Probele locale verifică boundary-ul; același purchase individual și prin suită
restrânsă verifică integrarea live. Un FAIL nou rămâne în feedback, chiar dacă
altă lansare trece. Detaliile normative sunt în [contractul modulului](../../../../../../README.md).

Continuă orientarea în [atlasul de context ATF](../../../../../../../docs/ATF_Context_Atlas.md).
