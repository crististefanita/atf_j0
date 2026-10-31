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

**Prioritate: 89/100** = impact 35 + lacună 27 + lucrabil 19 + documentare 8.
**Owner:** Reporting Tests. Criteriul API rămâne la
[Reporting](ATF_Reporting_Feedback.md#report-artifact-profile-62--aplicabilitate-explicită-a-inspectorului).

Consumerul J2 a reprodus pe J6 refuzul unui raport Extent exclusiv cu
assertions și aplicarea contractelor Extent pe fallback. Transferați cazul
într-un fixture neutru al producătorului, fără cod ori expected-uri Eventuate.

- [ ] Publicați assertion-only și HTTP, individual și agregat, în ambele renderere;
      fixați identitatea scenariului, fazelor și comparațiilor așteptate.
- [ ] Refuzați eliminarea unui request obligatoriu, a unui tabel ori a ultimei
      comparații și inversarea unui verdict. Un script sau un sibling PASS
      nu poate satisface inventarul scenariului degradat.
- [ ] Probați un mismatch după caracterul 240 și o comparație numerică FAIL;
      verificați un singur diagnostic al cauzei și deep-link-ul la rândul original.
- [ ] Rulați aceeași matrice pe sursă și pe JAR-ul fizic; validați browserul,
      inclusiv Enter/Space, wrapping și lățimea disponibilă, înaintea închiderii.

**Închidere:** profilul nominal este verde, fiecare mutant este refuzat de
oracolul potrivit, iar raportul și hash-ul binarului sunt legate de execuția
curentă. Cele 12 artifacte J2 nu substituie regresia permanentă a producătorului.


## Timing capturat versus publicare — regresie de păstrat

[REPORT-RECORDED-TIMING-83](ATF_Reporting_Feedback.md#report-recorded-timing-83--execuție-capturată-și-cost-de-publicare) are dovadă observată în consumerul J2, fără regresie permanentă închisă încă. Fixture-ul neutru trebuie să întârzie controlat publicarea după capturarea fazei și să verifice exact relațiile de timp în Extent/fallback, din sursă și JAR.

Oracolul trebuie să refuze implementarea care atribuie costul de publicare execuției, fără sleep-uri probabilistice sau expected-uri derivate din același formatter. Păstrați assertions, verdicturile, expandările și preview-ul 240.

| Revalidare Core C1 | Limită de acceptanță |
|---|---|
| J2 agregat: TEST BODY 109 ms, request copil 185 ms | [Dovada la owner](ATF_Reporting_Feedback.md#report-recorded-timing-83--execuție-capturată-și-cost-de-publicare) fixează artifactul și hash-ul |
| native Core și expandări corecte | nu închid regresia temporală Reporting; fixture-ul neutru rămâne necesar |


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

## REPORT-J7-GOVERNANCE-100 — coeziune și lizibilitate probate pe source

**Prioritate:** 87/100 = impact 34 + lacună 26 + lucrabil 19 + documentare 8.
**Owner:** modelul și regresiile generice Reporting la J0.

Guvernanța completă a reprodus patru eșecuri existente în snapshot-ul inițial:
modelul Reporting are nouă clase, pachetul de audit are zece teste și două
probe observation folosesc apeluri ATF complet calificate. Inventarul finit
J7, separat, este reconciliat cu testul Observation deja livrat.

| Suprafață | Criteriu de rezolvare |
|---|---|
| `atf.reporting.model` | revedeți coeziunea celor nouă tipuri și compatibilitatea API înaintea separării; nu măriți implicit pragul opt și nu mutați tipuri publice fără plan de livrare |
| `atf.reporting.audit` | separați observațiile/regresiile după responsabilitate, în source-set-ul de test J0; păstrați probele și numărul de module |
| observation tests | importați normal API-urile ATF în locul apelurilor complet calificate; nu alterați expected-ul ori verdictul |
| acceptanță | cele trei guard-uri vizate, nativele/regresiile Reporting și gate-ul JAR fizic trec; o schimbare de producție cere versiune și acceptanțe distincte la consumeri |

**Închidere:** cele patru eșecuri dispar prin structură/importuri revizuite,
cu probe comportamentale păstrate. Contractele Validation/profiluri/media/WER
au trecut separat; nu declarăm guvernanța completă J0 verde în acest increment.

[Model](../../atf-reporting/src/main/java/atf/reporting/model/ReportObservation.java) ·
[probe](../../atf-reporting-tests/src/test/java/atf/reporting/audit/ReportObservationComponentTest.java) ·
[guard](../../atf-tests/src/test/java/atf/api/tests/governance/AtfPackageCohesionGovernanceTest.java).
