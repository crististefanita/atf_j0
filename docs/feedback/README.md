# ATF Java — registrul cerințelor rămase

Registrul indexează criteriile neîndeplinite. Contractele stabile aparțin
README-ului ownerului; controalele verificate aparțin baseline-ului testelor.

## Pe scurt

- UI livrează politica BE read-only; rămân proiecția structurată, selectul înlocuit, izolarea subscriberilor și geometria iframe.
- Reporting are lifecycle-ul și timing-ul la [owner](../../atf-reporting/README.md#atomicitatea-tranzițiilor-de-scope); rămân fidelitatea query-ului și delimitarea publisherului.
- Consumerii au deschise cauzalitatea capturii order, identitatea/coșul la checkout, tranziția produsului AutoEx, runtime-ul E2E și billing-ul.
- Core, Validation, Console, Mock și Debug UI au [criterii reproduse la boundary](ATF_Code_Maintenance_Feedback.md), separate de afirmațiile editoriale.

În incrementul curent păstrăm cele 12 module și îmbunătățim codul existent:
responsabilități, stare, erori, duplicări și documentație. Flow-urile E2E Shop și
AutoEx pot fi completate în modulele lor până la rezultatul business declarat.

Propunerile de funcționalități noi și workflow-ul AI rămân în backlog;
scorul lor nu extinde scope-ul incrementului. Politica BE read-only livrată
explicit are contract la ownerii UI, Kit și consumeri.

## Top feedback-uri active

Scorul este prioritate de intervenție, nu notă a codului sau procent de acoperire.
Formula comună este impact /40 + lacună /30 + lucrabil /20 + datorie documentară /10.
Un document cu mai multe gap-uri primește scorul celui mai urgent, nu suma lor.

| Ordine | Feedback / owner | Scor /100 | Criteriul care decide ordinea |
|---:|---|---:|---|
| 1 | [Reporting — audit query și publicare](ATF_Reporting_Feedback.md) | **83** = 35 + 24 + 17 + 7 | query incomplet acceptat (83), publisher (64) |
| 2 | [Consumer — dovezi E2E și runtime](ATF_Consumer_Business_Test_Focus_Feedback.md) | **82** = 34 + 24 + 17 + 7 | începutul cererii order (82), identitate/coș (79), runtime Shop (77), billing (71), tranziție produs AutoEx (70), profile (57) |
| 3 | [Boundary-uri reutilizabile — cauză, scope și resurse](ATF_Code_Maintenance_Feedback.md) | **81** = 33 + 23 + 18 + 7 | diagnostic Core (81), cauză Validation (80), identitate query (79), context Console (77), close Debug (73), refuz one-shot (72) |
| 4 | [UI — sincronizare și recuperare BE](ATF_UI_Backend_Guided_Recovery_Feedback.md) | **78** = 33 + 22 + 16 + 7 | proiecție typed (78), select înlocuit (74), livrare către subscriberi (69) |
| 5 | [UI — geometrie browser](ATF_UI_Browser_Geometry_Feedback.md) | **72** = 31 + 25 + 10 + 6 | pointer în iframe la zoom CSS non-nativ |

O componentă verde confirmă criteriul ei; închiderea unui gap cere întreaga
probă declarată la owner. Evaluarea calității folosește separat
[rubrica documentației](../README_Atf_DocumentationBestPractices.md#rubrica-reviziei-editoriale).

## Traiectoria modulelor peste 91

Ținta privește codul livrat, conform [rubricii de cod](../README_Atf_CodeBestPractices.md#rubrica-reevaluării-modulelor).
O notă editorială mare nu compensează un false PASS, o cauză pierdută sau un
scope contaminat; reevaluarea urmează probei și sincronizării live.

| Modul | Criteriul care precede reevaluarea | Owner al feedback-ului |
|---|---|---|
| Core | diagnosticul defect nu schimbă dispatch-ul, rezultatul sau snapshot-ul | [izolarea diagnosticului](ATF_Code_Maintenance_Feedback.md#core-diagnostic-isolation-01--diagnosticul-nu-înlocuiește-execuția) |
| Mock | query-ul și constrângerile distincte nu se reasociază printr-o etichetă ambiguă | [identitate structurată](ATF_Code_Maintenance_Feedback.md#api-query-identity-01--identitate-structurată-separată-de-eticheta-afișată) |
| Validation | cauza inițială supraviețuiește close-ului; refuzul one-shot precede callback-ul | [cauza primară](ATF_Code_Maintenance_Feedback.md#validation-primary-cause-01--evaluare-publicare-și-închidere-distincte) și [refuzul](ATF_Code_Maintenance_Feedback.md#validation-one-shot-01--refuz-înaintea-callback-ului) |
| Console | copilul restaurează contextul/sink-ul părintelui la succes, FAIL și SKIP | [scope imbricat](ATF_Code_Maintenance_Feedback.md#console-nested-scope-01--restaurarea-contextului-exterior) |
| Reporting | auditorul refuză query-ul greșit; tabelele și publisherul păstrează semantica | [criterii Reporting](ATF_Reporting_Feedback.md) |
| UI | livrare izolată, identitate de captură și activare unică, fără replay nesigur | [sincronizare](ATF_UI_Backend_Guided_Recovery_Feedback.md) și [geometrie](ATF_UI_Browser_Geometry_Feedback.md) |
| Debug UI | close se termină bounded și cu request incomplet; rămâne read-only loopback | [shutdown](ATF_Code_Maintenance_Feedback.md#debug-bounded-close-01--închidere-cu-client-incomplet) |
| Shop | order cauzal, billing cu expected independent și runtime separat de wiring JUnit | [criterii consumer](ATF_Consumer_Business_Test_Focus_Feedback.md) |
| AutoEx | identitate/coș verificate după înregistrare, înaintea mutației checkout | [precondiția business](ATF_Consumer_Business_Test_Focus_Feedback.md#ui-locator-readiness-01--identitate-și-coș-înaintea-checkout-ului) |
| Reporting Tests | oracole independente pentru query, reasociere și randare; probele greșite sunt refuzate | [acceptanță Reporting Tests](ATF_Reporting_Feedback.md#acceptanță-pentru-reevaluarea-reporting-și-reporting-tests) |
| Test Kit | revalidarea protocolului JUnit și a binding-ului; reutilizare neutră numai după două utilizări reale | [limita reutilizării](ATF_Code_Maintenance_Feedback.md#reutilizare-și-finalizare) |
| ATF Tests | păstrarea gate-urilor și a probelor lor negative; nicio relaxare pentru lucrările de mai sus | [guvernanța ownerului](../../atf-tests/README.md) |

Kit și guvernanța nu primesc defecte inventate pentru a completa tabelul.
Fiecare modul se revede pe propriul boundary; fără module noi, clase auxiliare
fără motiv semantic sau funcționalități suplimentare pentru punctaj.

## Contracte și controale verificate

Regulile demonstrate sunt la README-ul ownerului și în
[ghidul de testare](../ATF_Testing_Guide.md); registrul de feedback nu dublează
inventarul lucrărilor închise. Controalele Git verificate sunt în
[baseline](../ATF_Test_Changes_Baseline.md#controale-tehnice-verificate),
iar cele `ACTIVE` rămân lângă owner până la proba lor completă.

## Design AI — evaluare separată

[Feedback-ul AI](README_AI_Evaluation.md) separă comparatorul implementat de
publication path, run ledger și workflow-ul propus. Readiness-ul său nu este
prioritatea operațională de mai sus și nu se recalibrează editorial.

## Păstrare, promovare și verificare

- Regula stabilă se promovează la owner înainte de eliminarea criteriului închis.
- Fișierul fără gap-uri se elimină după reconcilierea controalelor și linkurilor;
  țintele `@DocumentationTarget` rămân protejate.
- README-urile de modul rămân documentație de utilizare chiar fără ToDo-uri.

Protocolul este în [bunele practici documentare](../README_Atf_DocumentationBestPractices.md#păstrarea-și-ștergerea-feedback-ului).
Verificările respectă [staging-ul și sync-ul sigur](../ATF_Build_And_Execution_Safety.md).

## Cum verifici un criteriu

| Pas | Sursă | Rezultat cerut |
|---|---|---|
| Delimitează problema | secțiunea feedback a ownerului | trigger, observație, expected independent și boundary |
| Verifică implementarea | README owner → cod/POM → test nominal și negativ | criteriul este demonstrat, nu doar descris |
| Reconciliază controlul tehnic | [baseline testChanges](../ATF_Test_Changes_Baseline.md) și [gate-ul Git](../../atf-tests/src/test/java/atf/api/tests/governance/changes/TestChangeGovernanceTest.java) | control eligibil pentru diferența și baza de review curente |
| Închide | [protocolul documentar](../README_Atf_DocumentationBestPractices.md#păstrarea-și-ștergerea-feedback-ului) | regula stabilă este la owner, iar gap-ul închis nu rămâne backlog |

Scorul de prioritate nu este nota README-ului și nu certifică implementarea.
Designurile AI păstrează [evaluarea lor separată](README_AI_Evaluation.md),
fără a extinde incrementul de mentenanță al codului existent.
