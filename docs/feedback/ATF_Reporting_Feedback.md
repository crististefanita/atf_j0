# ATF Reporting — prezentarea și publicarea dovezilor

## Pe scurt

Criteriile lifecycle și timing au contracte și regresii la owner. Rămân
păstrarea cauzei la publicare, delimitarea publisherului și revenirea directă la dovadă; nu se extind funcționalitățile ori numărul modulelor.

## REPORT-EVIDENCE-RESPONSIBILITIES-20 — delimitarea protocoalelor de publicare

**Prioritate: 81/100** = impact 33 + lacună 24 + lucrabil 17 + documentare 7.
**Owner:** Reporting în main; probele neutre rămân în Reporting Tests.

`UiEvidencePublisher` coordonează setup-ul, flow-ul unificat în fază și peste
faze, request-urile API imbricate, capturile și failure evidence. Motivele de
schimbare sunt distincte: `publishApiEvidence()` depinde de schema exchange-ului,
`publishCrossPhaseUnifiedFlow()` de gruparea checkpoint-urilor, iar
`publishScreenshot()` de politica de captură. Fațada și lifecycle-ul există deja;
restanța nu se rezolvă prin mutarea lor într-un nou modul sau într-un helper generic.

| Probă de port `publishOutcome(...)` | Rezultat observat |
|---|---|
| Request cu eroare controlată la `detail(...)` și alta la `close()` | exact `detail → close`, fără repetarea publicării |
| Cauza primită de caller | numai close-ul; eroarea publicării lipsește din cause/suppressed |
| Limita reproducerii | protocolul publisherului cu port controlat, fără incident pretins într-un renderer real |

- [ ] Conservați eroarea inițială a publicării și atașați close-ul defect ca
      suppressed, fără self-suppression sau repetarea operației. Verificați
      separat body, complete și close, inclusiv eșecuri simultane.

- [ ] Extrageți mai întâi proiecția API într-un colaborator coeziv, păstrând
      aceeași fațadă și aceleași date evaluate; separați apoi gruparea flow-ului
      numai dacă rămâne un motiv independent de schimbare.
- [ ] Politica pură primește modele typed; publicarea primește porturi Reporting.
      Nu introduceți browser, JUnit, Shop ori AutoEx în producția librăriei.
- [ ] Păstrați snapshot-ul semantic, ownership-ul UI/API, FAIL/SKIP, markerii de
      timing, fazele, capturile și limita 240; fără reevaluarea comparației din text.
- [ ] Comparați Extent/fallback pe succes, failure, flow peste faze și publicare
      eșuată; verificați expandările și raportul business unic individual/prin suită.

**Închidere:** aceeași cauză inițială supraviețuiește cleanup-ului; protocoalele
pot fi schimbate și probate separat, fără o fațadă mai mare sau un API public
suplimentar. Contractele randate rămân identice.

## REPORT-FAILURE-DEEP-LINK-29 — revenire la dovada eșecului

**Prioritate: 75/100** = impact 30 + lacună 22 + lucrabil 17 + documentare 6.
**Owner:** navigarea runtime în Reporting; probe neutre în Reporting Tests.

În cele două HTML-uri Shop controlate prin clasa E2E existentă, 503 repetat și
200 fără produse rămân FAIL, cu acțiunea originală și observațiile catalogului.
`Fails → Open in Tests` deschide scenariul corect, dar faza `TEST BODY`, pasul
și validarea rămân colapsate. Comparația nu devine vizibilă fără alte clickuri.

- **Rutare:** [proiecția Fails](../../atf-reporting/src/main/resources/atf/reporting/javascript/extent-failure-projection.js)
  transmite numai clasa și indexul scenariului către `atfOpenTest`.
  `data-atf-failure-ordinal` există în markup, însă callback-ul nu îl folosește
  pentru deschiderea dovezii exacte.
- **Limita helperului:** expansiunea `ui-validation` existentă alege prima
  validare din `TEST BODY`; nu rezolvă general SETUP, TEARDOWN, un al doilea
  checkpoint sau ownerul API/component.
- **Limita probelor:** [UiTimeoutReportProbe](../../atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/failurecontext/UiTimeoutReportProbe.java)
  folosește `Expand all` după backlink, iar
  [UiFailureContextProbe](../../atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/failurecontext/UiFailureContextProbe.java)
  redeschide explicit conținutul. Ele confirmă păstrarea dovezilor și pot trece
  când deschiderea automată a căii este defectă.

**Criterii rămase:**

- [ ] Leagați backlink-ul de ownerul și calea semantică a eșecului: scenariu,
      fază, request/pas, validare și comparația originală. Nu alegeți primul
      nod din body și nu reconstituiți identitatea prin textul trunchiat.
- [ ] Verificați vizibilitatea întregii căi imediat după click, înainte de
      `Expand all` sau de un helper care deschide ancestorii.
- [ ] Probați separat SETUP/TEST BODY/TEARDOWN, owner UI/API/component,
      checkpoint ulterior și scenariu vecin PASS; includeți 392/1280 px,
      navigare repetată, ordinalul exact și toate coloanele accesibile.
- [ ] Adăugați control negativ care păstrează scenariul vizibil, dar închide
      faza ori validarea. Oracolul trebuie să îl refuze fără a elimina
      verificările de conținut, plafon 240, redactare și verdict existente.

**Închidere:** clickul Fails deschide dovada originală până la comparație,
cu ancestorii corecți și fără asistență din harness; scenariile vecine rămân neschimbate.

## Contracte existente

| Informație | Owner |
|---|---|
| Tranziții de scope și refuz fără mutație | [Reporting](../../atf-reporting/README.md#atomicitatea-tranzițiilor-de-scope) |
| Timing explicit, capturi și publicare | [Reporting](../../atf-reporting/README.md#timing-e2e-la-ownerul-runtime) |
| Setup și dovezi UI în fallback | [Reporting](../../atf-reporting/README.md#evidence-ui-și-setup-în-fallback) |
| Coloane dinamice și scroll local | [Reporting](../../atf-reporting/README.md#lizibilitatea-dovezilor-în-panouri-înguste) |
| Probe de compatibilitate și browser | [Reporting Tests](../../atf-reporting-tests/README.md) |
| Inventar UI/API/UI+API și controale negative de randare | [Reporting Tests](../../atf-reporting-tests/README.md#tabele-publicate-și-inventar-independent) |
| Controale Git verificate | [Baseline](../ATF_Test_Changes_Baseline.md#controale-tehnice-verificate) |
| Criterii active ale consumerilor | [Consumer](ATF_Consumer_Business_Test_Focus_Feedback.md) |
| Identitatea query-ului între modele | [Reporting Tests](../../atf-reporting-tests/README.md#fidelitatea-query-ului-și-asocierea-mock) |

## Acceptanță pentru reevaluarea Reporting și Reporting Tests

- Fidelitatea query-ului și asocierea cu sursa au [contract la owner](../../atf-reporting/README.md#fidelitatea-contractului-query);
  refactorizarea publisherului păstrează aceste oracole și probele negative.
- Publisherul se refactorizează după fixarea oracolelor, păstrând aceeași fațadă,
  snapshot-uri, ownership, timing și verdict. Regresia nu împrumută expected-ul
  din helperul pe care încearcă să îl verifice.
- Reporting Tests păstrează fixture-uri neutre și probe negative de control:
  inputul greșit este refuzat, iar schimbarea unei asocieri sau ascunderea unui
  rând este detectată. Nu mutați aceste probe în testele business Shop/AutoEx.

Nota se revede numai după [criteriul comun de cod](../README_Atf_CodeBestPractices.md#rubrica-reevaluării-modulelor),
cu randare inspectată și regresiile ownerului trecute în TMP.
