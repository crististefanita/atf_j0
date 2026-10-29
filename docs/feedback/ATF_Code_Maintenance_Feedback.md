# ATF — robustețea boundary-urilor existente

## Pe scurt

Acest feedback deține lacune reproduse prin porturile publice Core, Validation,
Console, Mock și Debug UI. Incrementul păstrează cele 12 module și API-urile
existente; nu introduce funcționalități noi pentru a justifica o notă mai mare.

Probele folosesc transport în memorie, callback-uri controlate și HTTP loopback.
Nu descriu incidente ale serviciilor publice Shop/AutoEx. Fidelitatea query-ului
în auditorul Reporting are [criteriu separat](ATF_Reporting_Feedback.md#report-query-fidelity-23--query-incomplet-validat-ca-fiind-conform).

## Ordinea intervențiilor

Scorul este prioritate: impact /40 + lacună /30 + lucrabil /20 + datorie
documentară /10. Nu reprezintă nota modulului și nu crește prin închiderea formală a unui checkbox.

| ID | Prioritate /100 | Diferența demonstrată |
|---|---:|---|
| `CORE-DIAGNOSTIC-ISOLATION-01` | 81 = 33 + 23 + 18 + 7 | răspunsul 200 se pierde pentru caller dacă sink-ul de diagnostic aruncă la COMPLETE |
| `VALIDATION-PRIMARY-CAUSE-01` | 80 = 33 + 22 + 18 + 7 | close înlocuiește failure-ul strict, fără cause/suppressed |
| `API-QUERY-IDENTITY-01` | 79 = 32 + 22 + 18 + 7 | chei neechivalente și reasocierea greșită a unei rute într-un rezultat declarat conform |
| `CONSOLE-NESTED-SCOPE-01` | 77 = 30 + 22 + 18 + 7 | wrapperul dinamic imbricat elimină contextul părintelui |
| `DEBUG-BOUNDED-CLOSE-01` | 73 = 28 + 20 + 18 + 7 | close așteaptă un client care nu termină headerele |
| `VALIDATION-ONE-SHOT-01` | 72 = 27 + 20 + 18 + 7 | check evaluează predicatul înainte să refuze tabelul publicat |

## CORE-DIAGNOSTIC-ISOLATION-01 — diagnosticul nu înlocuiește execuția

**Owner:** Core, la apelarea portului neutru; Console deține numai adaptorul său.
Sursa este [ApiExecutor](../../atf-core/src/main/java/atf/core/http/execution/ApiExecutor.java).

Proba construiește un executor cu transport în memorie care întoarce HTTP 200
și un sink care aruncă aceeași excepție controlată la COMPLETE. Transportul se
execută o dată, dar callerul primește excepția sink-ului, fără rezultat și fără
snapshot de execuție. Protecția adaptorului Console nu acoperă orice implementare a portului.

- [ ] Delimitați publicarea diagnostică într-un colaborator neutru sau într-o
      politică locală coezivă; un sink defect nu oprește dispatch-ul valid și nu
      înlocuiește rezultatul unei execuții deja terminate.
- [ ] La failure funcțional, conservați aceeași cauză și snapshot-ul parțial;
      eroarea diagnostică secundară rămâne identificabilă conform politicii
      explicite, fără recursie prin sink-ul care tocmai a eșuat.
- [ ] Probați START, DETAIL, CODE și COMPLETE defecte înainte/după dispatch,
      succes, transport eșuat, timeout, anulare și retry deja planificat.
      Numărul apelurilor, deadline-ul și istoricul nu se schimbă.
- [ ] Păstrați scope-urile Maven și redacția existentă; nu introduceți Console,
      Reporting, JUnit sau un fallback stdout în producția Core.

**Închidere:** defectul sink-ului nu poate schimba rezultatul execuției ori
elimina cauza funcțională. Regresiile adiacente rămân la
[ApiTerminalExecutionEvidenceTest](../../atf-core/src/test/java/atf/core/http/execution/ApiTerminalExecutionEvidenceTest.java).

## VALIDATION-PRIMARY-CAUSE-01 — evaluare, publicare și închidere distincte

**Owner:** Validation, în
[ResponseContractReporter](../../atf-validation/src/main/java/atf/validation/publishing/ResponseContractReporter.java).

Cu expected HTTP 200 și actual 503, strict mode produce failure-ul contractului.
Un Scope al sink-ului care aruncă la close înlocuiește însă acea cauză:
excepția finală este close, fără cauza inițială în cause sau suppressed.
Failure-ul strict este deja un `ApiValidationException` derivat din AssertionError;
restanța demonstrată este închiderea din finally, nu o a doua publicare pe acest traseu.

- [ ] Separați decizia pură, publicarea și închiderea; conservați cauza primară
      și atașați erorile secundare fără self-suppression ori mutarea verdictului.
- [ ] Păstrați publicarea unică pe traseul strict și distincția dintre FAIL-ul
      cunoscut și RuntimeException din evaluare/publicare; repararea cleanup-ului
      nu introduce o scriere nouă într-un scope deja completat.
- [ ] Probați evaluator/publish/complete/close eșuate individual și simultan,
      strict, warning și `throwOnFailure=false`; pe traseul permis, callerul
      păstrează decizia și toate rândurile originale.
- [ ] Probele de port rămân în Validation; paritatea cu scope-urile reale
      Extent/fallback aparține Reporting Tests. Nu adăugați dependență către Kit
      ori renderer pentru a reutiliza o clasă de cleanup.

**Închidere:** aceeași cauză funcțională rămâne principală după un close defect,
fără al doilea verdict sau publicare tardivă. Proba existentă
[ResponseContractReporterTest](../../atf-validation/src/test/java/atf/validation/ResponseContractReporterTest.java)
folosește close fără eroare și nu demonstrează acest caz.

## CONSOLE-NESTED-SCOPE-01 — restaurarea contextului exterior

**Owner:** adaptorul JUnit Console; primitive neutre în Kit numai după
reutilizare demonstrată. Sursa este
[AtfConsoleTestContext](../../atf-console/src/main/java/atf/console/junit/AtfConsoleTestContext.java).

Două `dynamicTest(...).getExecutable().execute()` imbricate pe același thread
instalează corect contexte distincte. După copil, `clear()` lasă însă contextul
null, deși corpul părintelui continuă; sink-ul configurat al părintelui nu este restaurat.

- [ ] Tratați binding-ul ca scope restabilibil: salvați contextul anterior și
      restaurați-l în finally, inclusiv după FAIL și SKIP. Nu modificați politica
      părintelui prin fallback-ul global apărut în mijlocul invocării.
- [ ] Acoperiți și lansarea JUnit imbricată prin extensie, nu numai wrapperul
      dinamic; cleanup-ul unui copil nu elimină binding-ul ownerului exterior.
- [ ] Probați două niveluri, excepție, abort, publicare defectă și invocări
      paralele pe thread-uri distincte, cu OFF/SUMMARY/DETAILS și opt-in de path.
- [ ] Evaluați protocolul neutru deja folosit de
      [AuditedAssertionScope](../../atf-test-kit/src/main/java/atf/testkit/assertions/AuditedAssertionScope.java):
      acesta păstrează `previous` și ordinea close. Reutilizați numai mecanica
      identică, în Kit main, fără tipuri Console, Reporting sau business.

**Închidere:** după copil, `current()` și sink-ul sunt ale părintelui, iar după
ownerul exterior binding-ul este gol. Proba de concurență existentă
[Console integration](../../atf-console/src/test/java/atf/console/junit/AtfConsoleTestExtensionIntegrationTest.java)
nu substituie restaurarea imbricată pe același thread.

## API-QUERY-IDENTITY-01 — identitate structurată, separată de eticheta afișată

**Owner:** Mock și Reporting pentru modelele proprii; adaptorul de reasociere
aparține Reporting Tests. Sursele sunt
[MockRoute.key](../../atf-mock/src/main/java/atf/mock/MockRoute.java),
[ApiReportRouteContract.key](../../atf-reporting/src/main/java/atf/reporting/api/contract/ApiReportRouteContract.java)
și [MockReportContractAuditor](../../atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/audit/mock/MockReportContractAuditor.java).

| Input controlat | Rezultat curent |
|---|---|
| aceeași hartă `a=z`, `a-=b` | Mock: `a=z&a-=b`; Reporting: `a-=b&a=z` |
| o pereche `a → 1&b=2` față de două perechi `a → 1`, `b → 2` | aceeași cheie `a=1&b=2` în ambele modele |
| adaptor real cu ruta pentru `a=z`, `a-=b` | compliant=true, fără violations, dar ruta Mock returnată este absentă |
| două rute cu cheia în coliziune, body-uri `owner=first` și `owner=second`; URL-ul și body-ul observat sunt ale celei de-a doua | compliant=true, fără violations, dar ruta Mock returnată este prima |

Comparatorii de sortare diferă, iar concatenarea fără delimitare sigură nu este
injectivă. Proba prin `MockReportContractAuditor.inspect(...)` demonstrează
reasocierea greșită la boundary-ul real; nu pretinde un failure browser observat.

- [ ] Folosiți o identitate typed sau un mapping explicit al rutei selectate;
      eticheta lizibilă nu este identificator unic. Păstrați multiplicitatea,
      valorile goale și dimensiunile necesare pentru a distinge rutele configurate.
- [ ] Nu pierdeți proveniența prin `.findFirst()` peste etichete ambigue și nu
      modificați titlurile raportului ca efect secundar al corectării asocierii.
- [ ] Testați nume prefix, delimitatori, Unicode, `%`, `+`, duplicate identice
      și diferite, permutări și rute cu același verb/path, dar alte constrângeri.
      Expected-ul vectorilor nu folosește canonicalizerul din producție.
- [ ] Mock poate reutiliza Core; Reporting rămâne standalone. Conversia dintre
      modelele lor este la adaptor, fără dependență Reporting → Core/Mock.
- [ ] Verificați reasocierea și refuzul în Reporting Tests prin API-ul real al
      adaptorului; o comparație izolată între două string-uri nu închide integrarea.

**Închidere:** aceeași identitate business se asociază stabil, iar structurile
distincte nu se confundă prin eticheta produsă pentru afișare.

## DEBUG-BOUNDED-CLOSE-01 — închidere cu client incomplet

**Owner:** [DebugUiServer](../../atf-debug-ui/src/main/java/atf/debugui/DebugUiServer.java).
Un client loopback trimite request line și un header neterminat; o a doua
cerere completă confirmă că serverul acceptă trafic. Close închide listenerul,
dar așteaptă handlerul blocat în readLine; proba finalizează numai după eliberarea clientului.

- [ ] Delimitați ownership-ul conexiunilor acceptate și închideți-le la oprire,
      ori folosiți un deadline de citire explicit și o compensare echivalentă.
      Închiderea executorului nu substituie eliberarea I/O-ului blocat.
- [ ] Probați request line incomplet, headere incomplete, streaming întrerupt,
      mai mulți clienți și close repetat/concurent; listenerul și handler-ele
      deținute se termină fără a opri procese străine.
- [ ] Păstrați GET/HEAD, streaming-ul, rezolvarea realpath, refuzul traversal-ului,
      portul efemer și binding-ul exclusiv loopback. Nu construiți un server HTTP general.
- [ ] Proba folosește semnale de readiness și deadline de siguranță al testului;
      închiderea socketului de fixture rămâne cleanup, nu dovada că serverul și-a închis clienții.

**Închidere:** close se termină bounded fără cooperarea clientului defect.
[DebugUiServerTest](../../atf-debug-ui/src/test/java/atf/debugui/DebugUiServerTest.java)
rămâne regresie adiacentă, fără extinderea instrumentului într-un produs UI.

## VALIDATION-ONE-SHOT-01 — refuz înaintea callback-ului

**Owner:** [ValidationTable](../../atf-validation/src/main/java/atf/validation/evaluation/ValidationTable.java).
După `add(...)` și `decide(false)`, tabelul este published. Un `check(...)`
tardiv execută totuși predicatul o dată, apoi aruncă IllegalStateException;
colecția păstrează numai rândul inițial, dar callback-ul invalid a fost deja executat.

- [ ] Verificați lifecycle-ul și identitatea înaintea callback-ului, astfel
      încât refuzul unui input neeligibil să nu aibă efecte observabile.
- [ ] Delimitați excepția de evaluare de excepțiile de protocol; un callback
      legitim care aruncă păstrează rândul FAIL, fără reevaluare și fără a masca o eroare de scriere.
- [ ] Probați tabel publicat, identitate duplicată/invalidă și callback defect;
      numărul evaluărilor este zero pentru refuz și unu pentru comparația eligibilă.
- [ ] Păstrați oracolul integral, politica strict/warning, formatul comun și
      redacția înaintea limitei 240. Verificați tabela și verdictul la owner,
      fără UI sau HTTP sintetic în testul unitar.

**Închidere:** operația invalidă nu execută predicatul și nu modifică snapshot-ul;
operația eligibilă păstrează aceeași decizie publică.

## Reutilizare și finalizare

- Kit nu primește un defect fictiv pentru a justifica o extracție. Orice protocol
  comun de binding se extrage numai dacă înlocuiește mecanică identică în Console
  și auditul Kit, cu aceleași garanții de owner/thread/ordine și cauză primară.
- Producția reutilizabilă rămâne în main la owner; adaptoarele JUnit, fixture-urile
  și expected-urile rămân în test. Probele neutre de raportare aparțin Reporting Tests.
- Fiecare increment are probă nominală și negativă, [control Git eligibil](../ATF_Test_Changes_Baseline.md)
  unde este necesar și [build/sync în TMP](../ATF_Build_And_Execution_Safety.md).
- O notă peste 91 cere implementare demonstrată conform
  [rubricii de cod](../README_Atf_CodeBestPractices.md#rubrica-reevaluării-modulelor),
  nu mai multe clase, teste tautologice sau un README mai lung.
