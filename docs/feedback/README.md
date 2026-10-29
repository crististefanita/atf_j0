# ATF Java — registrul cerințelor rămase

Registrul indexează criteriile neîndeplinite. Contractele stabile aparțin
README-ului ownerului; controalele verificate aparțin baseline-ului testelor.

## Pe scurt

- UI livrează politica BE read-only; rămân proiecția structurată, selectul înlocuit, cleanup-ul capturii, coeziunea readiness și geometria iframe.
- Reporting păstrează [cauza publicării](../../atf-reporting/README.md#cauza-publicării-și-scope-urile-deținute) la owner; rămâne revenirea directă la comparația eșuată.
- Consumerii păstrează deschise marcajul numeric Shop, billing-ul, tranziția produsului AutoEx și probele achiziției parțiale.
- Core prevalidează sursele aprobate; [eligibilitatea părinților destinației](ATF_Core_Promotion_Feedback.md) rămâne de întărit.
- Validation, Console, Mock și Debug UI au contractele de boundary și probele la owner; registrul păstrează numai lacunele deschise.

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
| 1 | [UI — sincronizare și recuperare BE](ATF_UI_Backend_Guided_Recovery_Feedback.md) | **78** = 33 + 22 + 16 + 7 | proiecție typed (78), cleanup-ul capturii (77), select înlocuit (74), coeziune readiness (68) |
| 2 | [Reporting — navigarea la dovadă](ATF_Reporting_Feedback.md) | **75** = 30 + 22 + 17 + 6 | backlink-ul lasă faza, pasul ori validarea colapsate |
| 3 | [Consumer — dovezi E2E și runtime](ATF_Consumer_Business_Test_Focus_Feedback.md) | **73** = 28 + 21 + 18 + 6 | marcaj numeric (73), billing (71), tranziție produs AutoEx (70), achiziție parțială (61), profile (57) |
| 4 | [UI — geometrie browser](ATF_UI_Browser_Geometry_Feedback.md) | **72** = 31 + 25 + 10 + 6 | pointer în iframe la zoom CSS non-nativ |
| 5 | [Core — părinții destinației](ATF_Core_Promotion_Feedback.md) | **66** = 25 + 18 + 18 + 5 | un părinte devenit fișier este descoperit după prima scriere; sursele valide sunt deja prevalidate |

O componentă verde confirmă criteriul ei; închiderea unui gap cere întreaga
probă declarată la owner. Evaluarea calității folosește separat
[rubrica documentației](../README_Atf_DocumentationBestPractices.md#rubrica-reviziei-editoriale).

## Traiectoria modulelor peste 91

Ținta privește codul livrat, conform [rubricii de cod](../README_Atf_CodeBestPractices.md#rubrica-reevaluării-modulelor).
O notă editorială mare nu compensează un false PASS, o cauză pierdută sau un
scope contaminat; reevaluarea urmează probei și sincronizării live.

| Modul | Contract demonstrat sau criteriu rămas | Owner |
|---|---|---|
| Core | sursele sunt prevalidate; părinții destinației au criteriu separat | [promovarea guarded](../../atf-core/README.md#manifest-și-promovare-guarded) |
| Mock | query-ul și constrângerile distincte nu se reasociază printr-o etichetă ambiguă | [asocierea exactă în adaptor](../../atf-reporting-tests/README.md#fidelitatea-query-ului-și-asocierea-mock) |
| Validation | cauza inițială supraviețuiește close-ului; refuzul one-shot precede callback-ul | [cauza și protocolul](../../atf-validation/README.md#cauza-primară-și-refuzul-înaintea-evaluării) |
| Console | copilul restaurează contextul/sink-ul părintelui la succes, FAIL și SKIP | [scope imbricat](../../atf-console/README.md#restaurarea-contextelor-imbricate) |
| Reporting | query-ul și cauza publicării sunt conservate; revenirea la comparație rămâne deschisă | [criteriul Reporting](ATF_Reporting_Feedback.md) |
| UI | livrare izolată și start identity; cleanup-ul după dezabonare eșuată rămâne deschis | [sincronizare](ATF_UI_Backend_Guided_Recovery_Feedback.md) și [geometrie](ATF_UI_Browser_Geometry_Feedback.md) |
| Debug UI | close se termină bounded și cu request incomplet; rămâne read-only loopback | [shutdown](../../atf-debug-ui/README.md#închiderea-conexiunilor-deținute) |
| Shop | Confirm typed cauzal; compatibilitate numerică, billing și achiziție parțială rămase | [criterii consumer](ATF_Consumer_Business_Test_Focus_Feedback.md) |
| AutoEx | identitate/coș verificate după înregistrare, înaintea mutației checkout | [gate-ul business](../../atf-test-auto-ex/README.md#gate-ul-coșului-și-ownerul-contului) |
| Reporting Tests | oracole independente pentru query, reasociere și randare; probele greșite sunt refuzate | [contractele și probele](../../atf-reporting-tests/README.md#fidelitatea-query-ului-și-asocierea-mock) |
| Test Kit | revalidarea protocolului JUnit și a binding-ului; reutilizare neutră numai după două utilizări reale | [limita reutilizării](../../atf-test-kit/README.md) |
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
