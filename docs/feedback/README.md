# ATF Java — registrul cerințelor rămase

Registrul indexează criteriile neîndeplinite. Contractele stabile aparțin
README-ului ownerului; controalele verificate aparțin baseline-ului testelor.

## Pe scurt

- UI livrează politica BE read-only, continuitatea selectului, observerii, geometria iframe și release-ul sesiunii cu păstrarea cauzei; contractele sunt la owner.
- Reporting verifică sursa izolată, JAR-ul fizic și refuzul constructorului; [pregătirea pentru release](ATF_Reporting_Feedback.md) păstrează API-ul, distribuția și portabilitatea. [Reporting Tests](ATF_Reporting_Tests_Feedback.md) deține oracolele per scenariu și degradările focalizate.
- Consumerii păstrează verdictul, cauza și captura per invocare; ramurile Cart și eroarea după PASS au probe distincte. Shop rezolvă output-ul prin POM-ul propriu, identic cu Reporting sursă sau JAR.
- Core prevalidează sursele și părinții destinației fără scriere; [atomicitatea per fișier](../../atf-core/README.md#manifest-și-promovare-guarded) rămâne explicită.
- Documentația verifică linkurile și fragmentele în README-uri și docs; [afirmațiile factuale critice](../../atf-tests/README.md#documente-și-responsabilități) compară modulele, scope-ul Kit și geometria selectată cu sursa executabilă; restul sensului păstrează review-ul manual.
- Validation, Console și Kit păstrează contractele verificate; criteriile noi au owner și probă distinctă, fără refactorizări fabricate pentru punctaj.

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
| 1 | [Reporting — contracte și pregătire JAR](ATF_Reporting_Feedback.md) | **81** = 31 + 24 + 17 + 9 | API susținut (81), proiecția recovery (78), release (73), portabilitate (72), proiecția flow-ului (62) |
| 2 | [Reporting Tests — oracole și JAR](ATF_Reporting_Tests_Feedback.md) | **80** = 31 + 24 + 18 + 7 | dovadă distinctă per scenariu (80), cu degradări focalizate și identitate exactă |
| 3 | [Core — redactare și HTTP](ATF_Core_Boundaries_Feedback.md) | **76** = 30 + 23 + 17 + 6 | mască literală (76), media type (68), header explicit (65); Mock verifică efectul MIME |
| 4 | [Guvernanță — inventar și controale](ATF_Governance_Feedback.md) | **70** = 27 + 20 + 17 + 6 | surse compilabile omise (70); reconcilierea controalelor Git ale producătorului (64) |
| 5 | [Consumer — locale Shop](ATF_Consumer_Business_Test_Focus_Feedback.md) | **59** = 22 + 15 + 17 + 5 | încărcarea Shop în tr-TR |
| 6 | [Debug UI — path URI](ATF_Debug_UI_Feedback.md) | **42** = 14 + 9 + 14 + 5 | plusul literal selectează fișierul cu spațiu |

O componentă verde confirmă criteriul ei; închiderea unui gap cere întreaga
probă declarată la owner. Evaluarea calității folosește separat
[rubrica documentației](../README_Atf_DocumentationBestPractices.md#rubrica-reviziei-editoriale).

În perimetrul J1 rămân criteriile reproduse pentru masca literală,
MIME/headere, locale și identitatea path-ului Debug. Inventarul
guvernanței cere separarea output-ului de sursa compilabilă; probele nominale
existente nu certifică aceste negative noi.

- Proiecția typed `UI-RECOVERY-AUDIT-01` are owner Reporting.
- Prevenirea factuală selectivă are ownerul neutru ATF Tests și declarații la ownerul documentului.

## Traiectoria modulelor peste 91

Ținta privește codul livrat, conform [rubricii de cod](../README_Atf_CodeBestPractices.md#rubrica-reevaluării-modulelor).
O notă editorială mare nu compensează un false PASS, o cauză pierdută sau un
scope contaminat; reevaluarea urmează probei și sincronizării live.

| Modul | Contract demonstrat sau criteriu rămas | Owner |
|---|---|---|
| Core | promovarea guarded rămâne verificată; măștile literale, media type și precedența headerelor au negative noi | [boundary-uri Core](ATF_Core_Boundaries_Feedback.md) |
| Mock | identitatea rutelor și query-ul rămân distincte; MIME-ul fals JSON cere regresie la matching | [clasificarea Core și efectul Mock](ATF_Core_Boundaries_Feedback.md) |
| Validation | cauza inițială supraviețuiește close-ului; refuzul one-shot precede callback-ul | [cauza și protocolul](../../atf-validation/README.md#cauza-primară-și-refuzul-înaintea-evaluării) |
| Console | copilul restaurează contextul/sink-ul părintelui la succes, FAIL și SKIP | [scope imbricat](../../atf-console/README.md#restaurarea-contextelor-imbricate) |
| Reporting | source/JAR gate, constructor cu rollback, lifecycle declarat, query privat și ordinalul dovezii originale; API-ul susținut și release păstrează criterii distincte | [criteriile Reporting](ATF_Reporting_Feedback.md) |
| UI | pointerul iframe folosește context nativ; sesiunea păstrează cauza și eliberează toate resursele deținute o dată; observerii au ownership separat; proiecția typed rămâne la Reporting | [lifecycle](../../atf-ui/README.md#lifecycle-ul-sesiunii-și-limita-probei) și [geometrie](../../atf-ui/README.md#geometria-nativă-a-contextului) |
| Debug UI | close bounded rămâne verificat; plusul literal trebuie să păstreze identitatea fișierului | [path URI](ATF_Debug_UI_Feedback.md) |
| Shop | păstrează corelarea, output-ul și cleanup-ul; încărcarea profilului depinde încă de locale-ul JVM | [configurarea consumerului](ATF_Consumer_Business_Test_Focus_Feedback.md) |
| AutoEx | identitate și cleanup owned; suma completă și ambii adaptori păstrează FAIL-ul, cu nominal separat | [contractul facturii](../../atf-test-auto-ex/README.md#validarea-completă-a-sumei-facturii) |
| Reporting Tests | oracole independente pentru query, randare, backlink, achiziție parțială și cleanup direct/delegat | [contractele și probele](../../atf-reporting-tests/README.md#erori-simultane-la-publicare-și-close) |
| Test Kit | revalidarea protocolului JUnit și a binding-ului; reutilizare neutră numai după două utilizări reale | [limita reutilizării](../../atf-test-kit/README.md) |
| ATF Tests | gate-urile existente rămân; sursele compilabile cu nume de package rezervate excluderii cer reconciliere | [inventar și oracol](ATF_Governance_Feedback.md) |

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
