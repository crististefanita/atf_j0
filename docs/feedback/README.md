# ATF Java — registrul cerințelor rămase

Registrul indexează criteriile neîndeplinite. Contractele stabile aparțin
README-ului ownerului; controalele verificate aparțin baseline-ului testelor.

## Pe scurt

- Core trebuie să transporte istoricul parțial al unei execuții API eșuate.
- UI livrează politica BE read-only; rămân auditul agregat, readiness business și geometria iframe.
- Reporting păstrează contractul diagnosticului UI și timing-ul explicit al fazelor E2E.
- Consumerii păstrează corelarea payment, totalul comenzii, runtime-ul E2E Shop și billing-ul.
- Guvernanța trebuie să includă layout-urile Maven configurate în verificarea importurilor duplicate.

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
| 1 | [Core — execuție eșuată](ATF_Core_Execution_Evidence_Feedback.md) | **86** = 36 + 25 + 18 + 7 | istoricul retry-urilor nu este transportat la eroare terminală |
| 2 | [Consumer — validări E2E și runtime](ATF_Consumer_Business_Test_Focus_Feedback.md) | **83** = 34 + 24 + 19 + 6 | payment corelat cu răspuns vechi (83), context E2E (77), total comandă (72), billing (71), evaluator pur (63), profile (57) |
| 3 | [UI — sincronizare și recuperare BE](ATF_UI_Backend_Guided_Recovery_Feedback.md) | **79** = 32 + 23 + 17 + 7 | identitate/coș (79), audit agregat (78), select înlocuit (74) |
| 4 | [Reporting — diagnostic și timing E2E](ATF_Reporting_Feedback.md) | **78** = 32 + 21 + 18 + 7 | contractul diagnosticului (78), timing typed al fazelor E2E (78) |
| 5 | [UI — geometrie browser](ATF_UI_Browser_Geometry_Feedback.md) | **72** = 31 + 25 + 10 + 6 | pointer în iframe la zoom CSS non-nativ |
| 6 | [Mentenanță — inventarul surselor](ATF_Code_Maintenance_Feedback.md) | **70** = 24 + 19 + 20 + 7 | rădăcinile Shop omise din guard-ul importurilor duplicate |

O componentă verde confirmă criteriul ei; închiderea unui gap cere întreaga
probă declarată la owner. Evaluarea calității folosește separat
[rubrica documentației](../README_Atf_DocumentationBestPractices.md#rubrica-reviziei-editoriale).

## Contracte și controale verificate

Auditul componentelor și raportul comun class/suite sunt normate în
[ghidul de testare](../ATF_Testing_Guide.md#auditul-componentelor-consumerilor),
cu primitive la Kit și Reporting. Controalele demonstrate, inclusiv
`COMPONENT-SCOPE-AND-SELECTORS-01`, sunt în
[registrul verificat](../ATF_Test_Changes_Baseline.md#controale-tehnice-verificate).
Controalele rămase active sunt colapsate lângă feedback-ul ownerului.

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
