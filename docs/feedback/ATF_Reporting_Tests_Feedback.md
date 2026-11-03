# ATF Reporting Tests — oracole independente și acceptanță sursă/JAR

## Pe scurt

Acest feedback deține corectitudinea probelor din `atf-reporting-tests`:
oracol, fixture, classpath, navigare și acceptanța browserului.
[Implementarea bibliotecii](ATF_Reporting_Feedback.md) păstrează lifecycle-ul,
API-ul public, publicarea și release-ul; un test reparat nu închide implicit acel criteriu.

**Prioritate: 89/100** = impact 35 + lacună 27 + lucrabil 19 + documentare 8.
Un candidat de bibliotecă cere și probele canonice afectate, cu ieșirea integrală
păstrată în staging. O selecție focalizată verde nu certifică inventarul rezidual.

| Decizie obligatorie | Refuzul unei închideri premature |
|---|---|
| clasificați defectul înaintea modificării expected-ului | nu schimbați `component` în `execution` numai pentru verde; justificați ownerul cauzei și proba lui |
| oracolul citește dovada publicată a scenariului | textul găsit în script/CSS sau în alt scenariu nu dovedește verdict, captură ori comparație |
| verificați dovada exactă după navigare | tabul Tests vizibil și titlul comun `Failure evidence` nu confirmă faza și ordinalul |
| păstrați două limite de distribuție | runtime-ul JAR nu primește sursa producătorului pentru a satisface un fixture |
| controalele negative rămân executabile | eliminarea unei capturi, schimbarea unui verdict ori ascunderea dovezii originale trebuie detectate separat |


## REPORT-TEST-AUDIT-60 — o dovadă identificabilă pentru fiecare scenariu

**Prioritate: 80/100** = impact 31 + lacună 24 + lucrabil 18 + documentare 7.
**Owner:** Reporting Tests pentru fixture-ul neutru și oracol; consumerii
pentru asocierea fault-ului și expected-ul aplicației.

Căutarea unui marker în întregul HTML poate găsi template-ul JavaScript ori
dovada altui scenariu. Un raport comun cu mai multe negative cere identificarea
scenariului, fazei, ownerului, ordinalului și capturii originale.

- Auditul Shop confirmă și copii DOM ascunse după navigarea runtime: numărarea
  globală poate găsi 270 de rânduri, deși flow-ul activ afișează corect 135.
  Oracolul trebuie să identifice ownerul activ și comparația originală;
  vizibilitatea singură nu înlocuiește identitatea sau inventarul semantic.

- [ ] Publicați un fixture neutru cu negative și vecini PASS; comparați numărul
      și identitatea dovezilor în Tests, Fails și Overview, în Extent și fallback.
- [ ] Controlul negativ elimină numai captura unui caz, mută dovada la un
      sibling sau inversează numai verdictul lui; oracolul trebuie să refuze
      fiecare variantă fără să fie satisfăcut de script ori de alt scenariu.
- [ ] Pentru două diagnostice omonime verificați și ordinea inversă, reload,
      Enter/Space și grupul efectiv deschis. Nu acceptați asocierea după titlu.
- [ ] Păstrați preview-ul implicit 240 și bugetele configurate; comparația
      integrală precede redacția. Un mismatch după limita preview-ului rămâne
      FAIL fără publicarea integrală a valorilor ori expunerea unui secret.

**Închidere:** fiecare degradare focalizată are refuz observabil și diagnostic
cu identitatea dovezii. [Proba business a consumerilor](../ATF_Testing_Guide.md#acceptanța-business-individual-și-prin-suită)
rămâne la aplicație; fixture-ul neutru nu aduce Shop/AutoEx în Reporting Tests.

## Dovada cerută pentru handoff


| Livrabil | Criteriu |
|---|---|
| clasificare | implementare, test/oracol, fixture sau document; cauza și ownerul sunt explicite |
| regresie | nominal și negativ independent, fără praguri relaxate ori teste ascunse |
| distribuție | sursa izolată și consumerul JAR au classpath verificat; assets din același candidat |
| browser | fază și comparație exacte, două diagnostice omonime, tastatură, reload și panou îngust |
| acceptare | gate-urile relevante trec, criteriul demonstrat se elimină, controlul Git verificat trece în baseline |

Contractele și selecțiile sunt la [README-ul modulului](../../atf-reporting-tests/README.md).
Build-ul și auditul respectă [procesul izolat](../ATF_Build_And_Execution_Safety.md);
documentele normative nu păstrează artifacte sau rezultate datate.

## REPORT-TEST-ARTIFACT-PROFILE-62 — matrice neutră fără HTTP fictiv

Matricea permanentă este în [README-ul harness-ului](../../atf-reporting-tests/README.md#probe-inspector-timing-și-indicator-assertions), cu [acceptanță J12](../ATF_Reporting_J12_Verification.md). Assertion/observation-only și HTTP obligatoriu, individual/agregat, Extent/fallback, mismatch numeric și după 240, inventar/verdict/owner degradat: criteriul de profil este consolidat. Auditul generic exact-row/reload rămâne în REPORT-TEST-AUDIT-60.

## Timing capturat versus publicare — regresie de păstrat

[RecordedPublicationTimingComponentTest](../../atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/timing/RecordedPublicationTimingComponentTest.java) deține regresia permanentă cu barieră, fără sleep-uri probabilistice. [J12](../ATF_Reporting_J12_Verification.md) verifică sursa și JAR-ul fizic; atribuirea duratei nu mai este un ToDo.

## REPORT-TEST-FALLBACK-RESULT-WIDTH-101 — geometria celulei RESULT

**Prioritate: 64/100.** [Defectul bibliotecii](ATF_Reporting_Feedback.md#report-fallback-result-width-101--badge-ul-statusului-depășește-coloana)
are reproducere J7: badge-ul Warning este mai lat decât coloana RESULT,
producând overflow mic, deși patru coloane pot fi afișate în panoul normal.

- [ ] Adăugați ApiValidationColumnWidthComponentTest cu AtfComponentTest și fixture neutru partajat cu testul de disclosure; integrați selecția în suita componentă și gate-ul JAR.
- [ ] Publicați separat PASS, FAIL și WARNING, cu vecini PASS și header sintetic lung; expected-ul verdictului este furnizat direct, fără parser MIME sau dependency către Validation.
- [ ] Măsurați în browser conținutul stilizat, padding-ul, celula și wrapperul la viewport 1280 px și panou îngust; tolerați doar rotunjirea subpixel, nu badge care depășește celula.
- [ ] Restrângeți deliberat numai RESULT într-un mutant de artifact; oracolul geometric îl refuză, chiar dacă toate valorile și verdicturile rămân identice.
- [ ] Păstrați accesul la ultima coloană și ultimul rând, wrapping și preview 240 în Extent/fallback; nu înlocuiți proba DOM cu substring în HTML sau cu un procent fix de coloană.

**Închidere:** regresia reproduce overflow-ul vechi și refuză mutantul, apoi
verifică sursa și JAR-ul real. Proba de valori din consumer rămâne acolo;
fixture-ul și oracolul generic de geometrie sunt deținute permanent în J0.

## REPORT-TEST-SQL-STEPS-116 — probe dedicate SQL

**Prioritate: 88/100** = impact 35 + lacună 26 + lucrabil 18 + documentare 9.
**Owner:** atf-reporting-tests pentru componentele neutre și oracolele de
randare; nativele modelelor/configurației aparțin atf-reporting. Aceste probe
sunt cerute pentru [contractul SQL nou](ATF_Reporting_Feedback.md#report-sql-steps-116--query-uri-și-rezultate-auditabile), nu implementate în acest increment documentar.

Fixture-urile primesc fapte SQL capturate, fără Eventuate, H2 sau conexiune
reală în harness. Execuția JDBC independentă și selecțiile individual/suită
rămân acceptanța J2; un fixture neutru nu dovedește tranzacțiile aplicației.

| Familie componentă propusă | Oracol independent obligatoriu |
|---|---|
| disclosure și identitate | query plus bind-uri exacte în faza corectă, inclusiv două titluri identice, vecin PASS și SQL error controlat |
| bugete și redacție | query 4.096, bind-uri 2.048, rezultate 16.384 și payload SQL cumulat 1.048.576; fiecare configurabil separat, fără leak în canale ascunse |
| tabel rezultate | zero/unul/multe rânduri, duplicate de rând și de etichetă coloană, ordine fixă, NULL/gol, întregi mari și zecimale exacte |
| lifecycle și ownership | cauză inițială, cleanup secundar, cancel și captură parțială, publicare refuzată/repetată și niciun callback JDBC ori assertion reevaluat |

Separați familiile în pachete coezive în limita existentă de opt clase;
nu creați un modul nou. AtfComponentTest și selecțiile explicite rămân vizibile;
un test contractual închis nu dispare din suita de regresie.

<details>
<summary><strong>Matricea de regresie și probele negative</strong></summary>

- Pentru fiecare buget verificați limita minus unu, limita exactă și limita
  plus unu, valoare mică validă și configurații invalide/overflow. Schimbarea
  limitei rezultatelor nu trebuie să modifice limita query-ului sau preview-ul 240.
- Numărați separat code points vizibile și payload-ul efectiv serializat.
  Includeți diacritice, emoji compuse, markup care amplifică escaping-ul,
  parametri sensibili și multe execuții care epuizează bugetul cumulat.
- Oracolul scanează DOM, JSON, script, atribute și indexul de căutare pentru
  full payload/secret interzis; markerul de omisiune este inclus în buget și
  documentul final rămâne valid. Verificați și publicarea concurentă.
- Includeți eroare SQL așteptată într-un test PASS și eroare neinterceptată
  într-un FAIL, SQLState/vendor code și cauza originală; nu deduceți verdictul
  numai din existența unui error field sau din culoarea tabelului.
- Un mismatch după toate limitele de preview rămâne FAIL. SQL-only legitim,
  assertion-only și combinația UI/API/SQL se verifică fără request-uri fictive;
  includeți setup eșuat înaintea body-ului și cleanup eșuat după body.
- Mutați numai query-ul, bind-urile, rândul ori verdictul la un sibling omonim;
  eliminați o coloană duplicată, rotunjiți un număr, ascundeți full payload-ul
  peste plafon ori scurgeți un secret. Fiecare mutant trebuie refuzat independent.
- În Extent și fallback inspectați toate disclosure-urile, lățimea disponibilă,
  ultima coloană/rând, scroll interior, Enter/Space, panou îngust, reload și
  linkurile Tests/Fails/Overview către aceeași execuție și cauză.

</details>

**Închidere:** componente permanente și controale negative, matrice pe sursă
izolată și JAR fizic fără sursa producătorului, plus audit browser pentru
ambele renderere. Receipt-ul leagă scenariul, faza, ID-ul execuției și limitele
efective; un marker găsit ori un screenshot singur nu închide criteriul.

J2 adoptă apoi noul candidat și extinde matricea proprie de 18 rapoarte SQL cu
query/bind-uri/schema efectiv capturate. Acceptanța anterioară J9 rămâne istorică.

### Intrări concrete pentru componentele J0 — 2026-11-01

- [Matricea J2](../../../atf_j2/docs/ATF_SQL_Live_Verification.md) furnizează acum query/binds/schema/rows typed și erori controlate, în 12 Extent/fallback. Adăugați fixture-uri neutre permanente pentru fiecare familie de mai sus, fără copierea H2 sau a suitei JDBC în Reporting Tests.
- Păstrați negativele query absent, ID diferit de metadata, DECIMAL rotunjit și rând/coloană duplicată pierdută. Adăugați cross-owner cu titluri identice și SQL-only fără HTTP; refuzul legacy J10 este istoric; J12 oferă profilul explicit, iar acceptoarele J2 îl adoptă separat.
- Verificați separat captura parțială JDBC, tăierea HTML configurată și testul PASS care verifică o eroare SQL așteptată. Un error field nu impune FAIL asupra comparațiilor care au trecut; eroarea neconsumată păstrează FAIL și cauza originală.

## Limite active după consolidarea J12

- J12 a închis invarianta recordurilor publice de inspecție prin negative permanente sursă/JAR; acestea nu mai sunt criterii deschise.
- Regresiile Core console/cancellation sunt native Core. Proiecția public JSON-string este comparată separat în harness, fără dependență runtime Reporting → Core.
- [Unicode și scope-uri Kit](../../../atf_j1/docs/feedback/ATF_Common_Test_Support_Feedback.md) sunt cerințe J1; aici rămâne numai integrarea neutrelor cu tabelul Reporting.
- SQL-STEPS-116, oracolul exact-row/reload și geometria RESULT rămân criterii distincte; probele verzi de profil nu le închid implicit.

[Acceptanța J12](../ATF_Reporting_J12_Verification.md) păstrează metadata, timing, indicatorul assertions și profilul explicit. [Core](ATF_Core_Feedback.md) deține schimbările de transport și politica redacției.

[Compoziția inline J15 verificată](../ATF_Reporting_J15_Verification.md) are regresii permanente; controlul 136 este în baseline. Semantica Android rămâne la consumer.

## REPORT-TEST-SCREENSHOT-RETURN-137 — contextul viewer-ului pentru PNG reutilizat

Familie permanentă livrată în [J16](../ATF_Reporting_J16_Verification.md): [ScreenshotReferenceComponentTest](../../atf-reporting-tests/src/test/java/atf/reporting/observation/ScreenshotReferenceComponentTest.java) și [ScreenshotViewerBrowserComponentTest](../../atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/screenshot/ScreenshotViewerBrowserComponentTest.java). Cei doi owneri și ultimul checkpoint după scroll lung păstrează bytes, caption local, selecția, nodurile și focusul; nu există dependență Android.

Resize verifică identitatea și checkpoint-ul vizibil după reflow; fără reflow se cere scroll-ul legal exact. Controlul negativ refuză resetarea la zero chiar când documentul se micșorează; deadline-urile și preview-ul 240 rămân neschimbate.

## REPORT-TEST-SCREENSHOT-VIEWPORT-138 — fit integral fără crop

[Fixture-ul neutru](../../atf-reporting-tests/src/test/java/atf/reporting/support/screenshot/ScreenshotViewerFixture.java) produce portrait/landscape/square, trei PNG canonice și 42 de referințe, fără date Whist sau device. Probe componente și browser sursă/JAR verifică originalele, geometria integrală, resize, fit/original-size și mouse/Enter/Space/Escape; caption-urile cu entity literal sunt decodate exact o dată.

[Acceptanța J16](../ATF_Reporting_J16_Verification.md) păstrează separat nativele, matricea extinsă, rerulările afectate și fiecare consumer. Familiile 137/138 sunt în baseline; cerințele de setup/input/oracol și detaliu adaptiv ale J4 nu devin teste de business în Reporting.


## REPORT-LARGE-NAVIGATION-139 — permanent retained-evidence regression

The source/JAR navigation, hidden-table mutation and timing-cache contracts are
verified in [J17](../ATF_Reporting_J17_Verification.md). The reviewed test-change
control is retained in the [baseline](../ATF_Test_Changes_Baseline.md).

- External large artifacts default to targeted assertions/PNG paths. J18 additionally permits at most two explicitly selected source owners, with complete semantic-tree and retained-evidence checks.
- Selectors are scoped to the Tests view,
so a hidden Fails copy cannot satisfy or invalidate the active-owner oracle.

The final inventory includes canonical PNG payloads. A one-character mutation
with unchanged link, caption and owner is rejected on source and physical JAR,
including targeted acceptance of the complete archived Android report.

## REPORT-COLD-READINESS-141 — permanent startup regression

[The J18 record](../ATF_Reporting_J18_Verification.md) preserves the failed
J17 baseline and rejected bootstrap-only candidate, followed by permanent
source/JAR contracts at the original deadlines.

| Family | Required proof |
|---|---|
| ColdStartReadinessComponentTest | five startup destinations, deep link, stable repeated load/native hrefs, truthful failure/recovery and fresh real controls |
| EvidenceTableVisibilityComponentTest | unchanged writes avoid glyph measurement; actual text/native cell-font changes remeasure; known hidden views avoid geometry until reveal |
| LargeReportNavigationBrowserComponentTest | at most two explicit external bulk owners; every semantic card opens; original comparisons and canonical PNG inventories remain, with payload-mutation negative |

Control 141 is [verified in the baseline](../ATF_Test_Changes_Baseline.md).
The existing navigation/visibility paths remain under control 139; the new
control owns only the cold-start class, without overlapping paths.

## REPORT-TIMELINE-TITLE-142 — permanent native-script contract

TimelineTitleEncodingComponentTest retains three neutral owner labels: quoted
multiline JSON, Windows backslashes and Unicode. It executes the actual Spark
timeline script, compares full keys and rejects browser errors; the unchanged
deadlines and five timestamp cases stay explicit.

The old binary fails all three cases. [J18 evidence](../ATF_Reporting_J18_Verification.md#native-timeline-labels)
and the [verified baseline](../ATF_Test_Changes_Baseline.md) retain the proof,
with no consumer business logic in the generic harness.
