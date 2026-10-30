# ATF Java — registrul cerințelor rămase

Registrul indexează criteriile neîndeplinite. Contractele stabile aparțin
README-ului ownerului; controalele verificate aparțin baseline-ului testelor.

## Pe scurt

- UI livrează politica BE read-only, continuitatea selectului, cleanup-ul observerilor și geometria nativă iframe; rămâne proiecția structurată a încercărilor la Reporting.
- Reporting are gate pentru sursa izolată și JAR-ul consumat extern; [pregătirea pentru release](ATF_Reporting_Feedback.md) păstrează criteriile pentru API, lifecycle, oracole și documentație.
- Consumerii corelează lookup-ul Shop cu răspunsul și DOM-ul curente; sincronizează autofill-ul și confirmă produsul după click; probele locale Account/Cart/Purchase păstrează failure-ul și cleanup-ul owned; integrarea lor prin extensia/suita E2E condiționează reducerea selecției.
- Core prevalidează sursele și părinții destinației fără scriere; [atomicitatea per fișier](../../atf-core/README.md#manifest-și-promovare-guarded) rămâne explicită.
- Documentația verifică linkurile și fragmentele în README-uri și docs; [afirmațiile factuale critice](ATF_Documentation_Feedback.md) cer comparație independentă cu sursa executabilă.
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
| 1 | [Reporting — contracte și pregătire JAR](ATF_Reporting_Feedback.md) | **86** = 33 + 26 + 19 + 8 | rollback al constructorului și lifecycle (86), oracole, backlink pentru diagnostice omonime și fixture-uri binare (83), API (81), release (73), documentație (72), proiecția flow-ului (62) |
| 2 | [UI — sincronizare și recuperare BE](ATF_UI_Backend_Guided_Recovery_Feedback.md) | **78** = 33 + 22 + 16 + 7 | proiecție typed a încercărilor la Reporting (78) |
| 3 | [Documentație — fidelitate factuală](ATF_Documentation_Feedback.md) | **55** = 20 + 14 + 15 + 6 | afirmații rămase despre geometrie, standalone și preview în perimetrul Reporting |
| 4 | [Consumer — echivalență business](ATF_Consumer_Business_Test_Focus_Feedback.md) | **47** = 22 + 8 + 12 + 5 | integrarea E2E a probelor locale Account/Cart/Purchase și alternarea profilelor înaintea reducerii selecției |

O componentă verde confirmă criteriul ei; închiderea unui gap cere întreaga
probă declarată la owner. Evaluarea calității folosește separat
[rubrica documentației](../README_Atf_DocumentationBestPractices.md#rubrica-reviziei-editoriale).

În perimetrul fără Reporting rămâne [echivalența failure-urilor business](ATF_Consumer_Business_Test_Focus_Feedback.md#consumer-e2e-profiles-01--variantele-de-evidence-și-acoperirea-business),
înaintea reducerii selecției. Compoziția readiness, anularea deterministă,
achiziția parțială și comparația document/POM au contracte și probe la owneri.

- Proiecția typed `UI-RECOVERY-AUDIT-01` are owner Reporting.
- Fidelitatea documentelor Reporting rămâne în același perimetru separat.

## Traiectoria modulelor peste 91

Ținta privește codul livrat, conform [rubricii de cod](../README_Atf_CodeBestPractices.md#rubrica-reevaluării-modulelor).
O notă editorială mare nu compensează un false PASS, o cauză pierdută sau un
scope contaminat; reevaluarea urmează probei și sincronizării live.

| Modul | Contract demonstrat sau criteriu rămas | Owner |
|---|---|---|
| Core | sursele și părinții sunt prevalidate fără scriere; execuția rămâne atomică per fișier | [promovarea guarded](../../atf-core/README.md#manifest-și-promovare-guarded) |
| Mock | query-ul și constrângerile distincte nu se reasociază printr-o etichetă ambiguă | [asocierea exactă în adaptor](../../atf-reporting-tests/README.md#fidelitatea-query-ului-și-asocierea-mock) |
| Validation | cauza inițială supraviețuiește close-ului; refuzul one-shot precede callback-ul | [cauza și protocolul](../../atf-validation/README.md#cauza-primară-și-refuzul-înaintea-evaluării) |
| Console | copilul restaurează contextul/sink-ul părintelui la succes, FAIL și SKIP | [scope imbricat](../../atf-console/README.md#restaurarea-contextelor-imbricate) |
| Reporting | source/JAR gate, redacție JSON semantică, query privat și cauză terminală indexată; API-ul susținut, lifecycle și release păstrează criterii distincte | [criteriile Reporting](ATF_Reporting_Feedback.md) |
| UI | pointerul iframe folosește context nativ; inspectorul, selectul și observerii își păstrează identitatea, activarea unică și cleanup-ul complet; proiecția typed rămâne la Reporting | [sincronizare](ATF_UI_Backend_Guided_Recovery_Feedback.md) și [geometria livrată](../../atf-ui/README.md#geometria-nativă-a-contextului) |
| Debug UI | close se termină bounded și cu request incomplet; rămâne read-only loopback | [shutdown](../../atf-debug-ui/README.md#închiderea-conexiunilor-deținute) |
| Shop | marcaje numeric/typed, Confirm și billing sincronizat; rollback-ul achiziției parțiale păstrează cauza și închide contextul nativ o dată | [criterii consumer](ATF_Consumer_Business_Test_Focus_Feedback.md) |
| AutoEx | identitate, produs și factură confirmate bounded; smoke-ul extensiei izolează profilele; failure-ul din fiecare punct business condiționează reducerea selecției | [gate și readiness](../../atf-test-auto-ex/README.md#gate-ul-coșului-și-ownerul-contului) și [criterii consumer](ATF_Consumer_Business_Test_Focus_Feedback.md) |
| Reporting Tests | oracole independente pentru query, randare, backlink, achiziție parțială și cleanup direct/delegat | [contractele și probele](../../atf-reporting-tests/README.md#erori-simultane-la-publicare-și-close) |
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

| Moment | Actualizarea cerută |
|---|---|
| înaintea refactorizării | confruntați feedback-ul cu sursa live; fixați triggerul, ownerul, observația și proba de închidere |
| în timpul incrementului | reconciliați imediat constatarea nouă, limita probei ori schimbarea de scope; separați analiza statică de reproducere |
| înaintea promovării | revalidați sursele live, probele și linkurile; un criteriu parțial rămâne deschis, cu restanța exactă |
| după acceptare | promovați contractul stabil la owner și eliminați numai criteriul demonstrat, cu controalele și ancorele reconciliate |

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
