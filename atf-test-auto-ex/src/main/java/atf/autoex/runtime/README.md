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
| failure business controlat | [acceptanța explicită](../../../../../test/java/atf/autoex/tests/e2e/failures/AutoExBusinessFailureAcceptance.java) | fixture în test; journey real, captură originală, cleanup al contului deținut și JUnit FAIL păstrat |
| configurație și sesiune de raport | [AutoExReportSession](AutoExReportSession.java) | factory și delegare către biblioteca Reporting |
| download-ul facturii | [AutoExInvoiceDownload](invoice/AutoExInvoiceDownload.java) | un click; recuperare numai pentru GET-ul invoice queue-full observat |
| verdictul sumei facturii | [validatorul pur](../business/invoice/AutoExInvoiceValidator.java) → [adaptorul](../support/checkout/AutoExInvoiceAssertions.java) | clauză completă, sintaxă înainte de BigDecimal și FAIL fidel; fără replay |
| confirmarea produsului după click | [AutoExProductTransition](catalog/AutoExProductTransition.java) | un pointer, Close explicit dacă reclama interceptează, GET deținut 2xx și numele configurat; un singur buget |
| reclama apărută după pointer | [AutoExVignetteHandler](../support/browser/AutoExVignetteHandler.java) | Close explicit, fără replay; child INFO în durata părintelui |
| readiness read-only | [navigarea AutoEx](navigation/AutoExReadOnlyNavigation.java) | politică specifică documentului și originii AutoEx |
| conversia traficului observat | [AutoExObservedExchangeMapper](AutoExObservedExchangeMapper.java) | păstrează query-ul typed, service-ul și disponibilitatea response-ului în Reporting |

## Invariante de păstrat

- Observația nu primește expected-ul clientului și nu autorizează checkout-ul.
  Textul întârziat și documentul înlocuit folosesc deadline-ul inițial.
- Tranziția produsului citește locatorul vizibil heading/queue-full o singură
  dată per observație și evită repetarea așteptării unui URL deja confirmat.
  Documentul și produsul rămân oracole distincte; un port Purchase refuzat
  oprește flow-ul înaintea verdictului business și a mutației coșului.
- `AutoExE2eRun` cere explicit configurația, browserul, faza și lifecycle-ul;
  o extracție runtime nu mută JUnit sau Test Kit în main.
- Browserul, publicarea, timpul și cleanup-ul sunt responsabilități distincte.
  Finalizarea păstrează cauza inițială și eșecurile distincte de cleanup.
  Dacă eroarea inițială a fost deja capturată, refuzul ulterior al cleanup-ului
  publică un diagnostic propriu fără captură nouă. Nu înlocuiește imaginea
  inițială și nu autorizează replay-ul; un refuz de publicare rămâne suprimat
  pe eroarea cleanup-ului. Primul failure produs chiar de cleanup urmează
  capturarea existentă înaintea închiderii browserului.
- Download-ul este o acțiune măsurată; un click reușit nu o confirmă. Numai
  evenimentul real autorizează citirea fișierului; validatorul verifică factura.
- Queue-full permite numai GET-ul exact al facturii same-origin, în bugetul și
  încercările existente. POST-ul, documentul străin și backoff-ul prea lung sunt refuzate.

## Probe înainte de schimbare

| Contract | Probă de pornire |
|---|---|
| produs și refuzuri | [AutoExProductTransitionComponentTest](../../../../../test/java/atf/autoex/tests/component/pages/AutoExProductTransitionComponentTest.java) |
| factură și refuzuri | [AutoExInvoiceDownloadComponentTest](../../../../../test/java/atf/autoex/tests/component/pages/AutoExInvoiceDownloadComponentTest.java) |
| identitate și checkout unic | [AutoExCustomerIdentityObservationComponentTest](../../../../../test/java/atf/autoex/tests/component/pages/AutoExCustomerIdentityObservationComponentTest.java) |
| boundary runtime | [AutoExRuntimeBoundaryComponentTest](../../../../../test/java/atf/autoex/tests/component/lifecycle/AutoExRuntimeBoundaryComponentTest.java) |
| ordinea lifecycle-ului | [AutoExRunLifecycleAdapterComponentTest](../../../../../test/java/atf/autoex/tests/component/lifecycle/AutoExRunLifecycleAdapterComponentTest.java) |

Probele locale verifică boundary-ul; același purchase individual și prin suită
restrânsă verifică integrarea live. Un FAIL nou rămâne în feedback, chiar dacă
altă lansare trece. Detaliile normative sunt în [contractul modulului](../../../../../../README.md).

Continuă orientarea în [atlasul de context ATF](../../../../../../../docs/ATF_Context_Atlas.md).

Mapperul folosește [contractul query Reporting](../../../../../../../atf-reporting/README.md#fidelitatea-contractului-query)
pentru identitatea sigură a agregatului. Captura browser elimină valorile și
transportă numai amprenta opacă; mapperul nu fabrică perechi din placeholder.

Politica endpoint-ului și alegerea checkpoint-ului rămân AutoEx; biblioteca
primește contracte neutre, fără dependență UI.
`ReportAssertionSummary` deține numai formatarea și redacția diagnosticului;
`AutoExE2eRun` păstrează comparațiile și decizia business.

## Limita probei de timp

Proba funcțională de tranziție folosește browserul după pregătirea fixture-ului,
cu deadline-ul acțiunii neschimbat. Aritmetica și propagarea deadline-ului sunt
verificate determinist la ownerul UI prin `ObservationBudgetComponentTest`;
durata de lansare ori închidere Chromium nu este un oracle al acelui contract.
