# ATF Reporting UI — de la checkpoint la porturile rendererului

## Pe scurt

Pachetul compune dovezi UI/API deja observate. Deține lifecycle-ul scenariului,
gruparea checkpoint-urilor și publicarea; browserul, endpointul și decizia
business aparțin consumerului.

[Atlas](../README.md) · [Contractul modulului](../../../../../../README.md#contractul-raportului)

| Motivul schimbării | Intrarea de citit | Responsabilitate |
|---|---|---|
| deschidere și profil rezolvat | [UiExtentReportSession](UiExtentReportSession.java), [opțiuni](UiExtentReportOptions.java) | fațada și compatibilitatea intrărilor |
| scenariu și unificare între faze | [UiScenarioLifecycle](UiScenarioLifecycle.java) | starea scenariului și checkpoint-urile reținute |
| pas, setup, timing și închiderea fazei | [UiPhaseLifecycle](UiPhaseLifecycle.java) | lifecycle-ul unei faze |
| proiecție și porturi de evidence | [UiEvidencePublisher](UiEvidencePublisher.java) | layout/checkpoint și flow compact |
| schimburi API ale checkpoint-ului | [UiApiValidationEvidence](validation/UiApiValidationEvidence.java), [publisher](UiEvidencePublisher.java) | `ApiEvidencePublisher` este colaborator privat nested |
| cauza eșecului și captura permisă | [publisher failure](failure/UiFailureEvidencePublisher.java) | diagnostic și publicare, fără captură nouă |

## Traseul unei modificări

- Consumerul transmite acțiuni, assertions și captura deja disponibile.
  `ReportEvidenceProfile` separă layout-ul, screenshot policy și disclosure-ul acțiunilor.
- Fațada păstrează intrările publice; lifecycle-urile gestionează starea, iar
  publisherii scriu în porturile `AtfReportPorts`.
- Flow-ul compact poate reține checkpoint-uri până la completare. Schimbarea
  grupării cere și contextul scenariului, nu numai metoda care publică pasul curent.

<details>
<summary><strong>Invariante și limite care se pierd ușor din context</strong></summary>

- Cross-phase unified flow se selectează înainte de prima fază și cere profil
  unificat. Nu schimbă ordinea faptelor sau verdictul primit de la consumer.
- `ReportPublicationScope` păstrează cauza și încearcă fiecare close deținut;
  failure-ul delegat are propriul request și propriul copil validation.
- Aserțiunile API ale unui exchange nu se reasociază cu vecinul după textul
  trunchiat. Un canal gol și un exchange fără response sunt situații distincte.
- Colaboratorul API este privat în `UiEvidencePublisher` și publică fiecare
  exchange valid, inclusiv fără response. Boundary-ul cere request și, fără
  response, verdict explicit ori aserțiuni evaluate; INFO singur nu îl furnizează.
  Diagnosticul și attempts rămân în Execution evidence, fără Response details inventat.
- Diagnosticul terminal este inspectabil în Tests. Pentru un FAIL fără assertions,
  [indexarea Fails](../../../../../../../docs/feedback/ATF_Reporting_Feedback.md#report-terminal-failure-index-48--cauza-transportului-în-indexul-fails)
  rămâne de corectat; nu deduceți cauza din ultimul exchange frate.
- Succesiunea Given/When/Then folosește contextul scenariului; continuările AND
  nu sunt o înlocuire globală a keyword-urilor în HTML.
- Timing-ul execuției și cel al publicării sunt fapte separate; rendererul nu
  execută din nou operația pentru a obține durate sau dovezi lipsă.

</details>

## Probe și criterii active

- `UiApiValidationContractComponentTest`: forma și asocierea canalului API.
- `ReportingStandaloneVerdictComponentTest` și `UiTerminalExchangeReportComponentTest`: refuzul
  succesului implicit, toate căile UI și paritatea diagnosticului Extent/fallback.
- `UiCrossPhaseCompactFlowComponentTest`: gruparea în/peste faze.
- `UiPublicationFailureComponentTest`: body, achiziție și close prin fațada publică.
- `UiPhaseTimingLifecycleComponentTest` și `ReportGherkinSequenceTest`: timing și vocabular.

Probele de compoziție și browser sunt în [Reporting Tests](../../../../../../../atf-reporting-tests/README.md).
Boundary-ul standalone este verificat în
[biblioteca Reporting](../../../../../../src/test/java/atf/reporting/standalone/ReportingStandaloneVerdictComponentTest.java).
API-ul susținut și extragerea proiecției pure au
[criterii distincte](../../../../../../../docs/feedback/ATF_Reporting_Feedback.md).

