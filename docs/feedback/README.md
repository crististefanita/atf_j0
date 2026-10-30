# ATF Java — registrul cerințelor rămase

Registrul indexează criteriile neîndeplinite. Contractele stabile aparțin
README-ului ownerului; controalele verificate aparțin baseline-ului testelor.

## Pe scurt

- UI livrează politica BE read-only; rămân proiecția structurată, selectul înlocuit, coeziunea readiness și geometria iframe.
- Reporting produce un JAR consumabil extern; [pregătirea pentru release](ATF_Reporting_Feedback.md) cere corectarea redactării JSON și izolării, împreună cu API-ul și documentația.
- Consumerii păstrează deschise billing-ul Shop, tranziția produsului AutoEx, probele achiziției parțiale și echivalența profilelor.
- Core prevalidează sursele și părinții destinației fără scriere; [atomicitatea per fișier](../../atf-core/README.md#manifest-și-promovare-guarded) rămâne explicită.
- Documentația are guard-uri structurale; [afirmațiile factuale critice](ATF_Documentation_Feedback.md) cer comparație independentă cu sursa executabilă.
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
| 1 | [Reporting — contracte și pregătire JAR](ATF_Reporting_Feedback.md) | **92** = 38 + 28 + 18 + 8 | redactare JSON (92), index Fails terminal (90), izolare (85), oracole și artifact (83), configurație (82), API (81), lifecycle (76), release (73), documentație (72), proiecția flow-ului (62) |
| 2 | [UI — sincronizare și recuperare BE](ATF_UI_Backend_Guided_Recovery_Feedback.md) | **78** = 33 + 22 + 16 + 7 | proiecție typed (78), select înlocuit (74), coeziune readiness (68) |
| 3 | [UI — geometrie browser](ATF_UI_Browser_Geometry_Feedback.md) | **72** = 31 + 25 + 10 + 6 | pointer în iframe la zoom CSS non-nativ |
| 4 | [Consumer — dovezi E2E și runtime](ATF_Consumer_Business_Test_Focus_Feedback.md) | **71** = 29 + 22 + 14 + 6 | billing (71), tranziție produs AutoEx (70), achiziție parțială (61), profile (57) |
| 5 | [Documentație — fidelitate factuală](ATF_Documentation_Feedback.md) | **63** = 23 + 17 + 17 + 6 | o coloană poate inversa sensul contractului și totuși trece guard-urile structurale |

O componentă verde confirmă criteriul ei; închiderea unui gap cere întreaga
probă declarată la owner. Evaluarea calității folosește separat
[rubrica documentației](../README_Atf_DocumentationBestPractices.md#rubrica-reviziei-editoriale).

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
| Reporting | JAR și assets consumate extern; terminalul în Tests cere verdict și păstrează diagnosticul; indexul Fails, redacția, API-ul, lifecycle și release rămân deschise | [criteriile Reporting](ATF_Reporting_Feedback.md) |
| UI | livrare izolată, start identity, atașare indivizibilă și cleanup la erori; proiecția typed și continuitatea selectului rămân deschise | [sincronizare](ATF_UI_Backend_Guided_Recovery_Feedback.md) și [geometrie](ATF_UI_Browser_Geometry_Feedback.md) |
| Debug UI | close se termină bounded și cu request incomplet; rămâne read-only loopback | [shutdown](../../atf-debug-ui/README.md#închiderea-conexiunilor-deținute) |
| Shop | marcaje numeric/typed și Confirm cauzale; billing și achiziție parțială rămase | [criterii consumer](ATF_Consumer_Business_Test_Focus_Feedback.md) |
| AutoEx | observația bounded, gate-ul identității și download-ul măsurat sunt probate; tranziția produsului și izolarea extinsă rămân de demonstrat | [gate și readiness](../../atf-test-auto-ex/README.md#gate-ul-coșului-și-ownerul-contului) și [criterii consumer](ATF_Consumer_Business_Test_Focus_Feedback.md) |
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
