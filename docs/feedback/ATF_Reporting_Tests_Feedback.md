# ATF Reporting Tests — oracole independente și acceptanță sursă/JAR

## Pe scurt

Acest feedback deține corectitudinea probelor din `atf-reporting-tests`:
oracol, fixture, classpath, navigare și acceptanța browserului.
[Implementarea bibliotecii](ATF_Reporting_Feedback.md) păstrează lifecycle-ul,
API-ul public, publicarea și release-ul; un test reparat nu închide implicit acel criteriu.

**Prioritate: 80/100** = impact 31 + lacună 24 + lucrabil 18 + documentare 7.
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
cu identitatea dovezii. [Proba business a consumerilor](ATF_Consumer_Business_Test_Focus_Feedback.md)
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
