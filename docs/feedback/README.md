# ATF Java — registrul cerințelor rămase

Registrul indexează numai gap-uri active. Contractele stabile aparțin README-ului
ownerului; rapoartele și rezultatele unei rulări nu devin documentație permanentă.

## Pe scurt

- Reporting: Timeline folosește duratele canonice din Tests; contractul și
  inspectorul comun sunt livrate la [owner](../../atf-reporting/README.md).
  Lizibilitatea tabelelor imbricate în panouri înguste rămâne deschisă.

- AutoEx: contractul livrat pentru Delete account, Close JavaScript la 70% și
  separarea navigării de traficul browser este în [README-ul modulului](../../atf-test-auto-ex/README.md#delete-account--navigare-întreruptă).
  Activarea DOM Close nu repară geometria pointerului; limita generică rămâne deschisă.
- Consumeri: scope-ul contului și runtime-ul main AutoEx sunt livrate.
  Pilotul coșului, factura și configurația evidence sunt livrate; rămân
  flow-urile din afara pilotului, corelarea mutațiilor și contextul Shop.
  Reducerea selecției live cere echivalență demonstrată.
- Contractele livrate se găsesc în [indexul documentației](../README.md),
  nu în checklist-uri de implementare deja închise.

## Top feedback-uri active

Scorul este o prioritate de intervenție, nu o notă a codului sau a README-ului.
Formula comună este impact /40 + severitatea lacunei /30 + lucrabil acum /20 +
datorie documentară /10; toate domeniile operaționale au aceeași pondere.

Un feedback cu mai multe gap-uri primește scorul celui mai urgent, nu suma
lor. Gap-urile secundare rămân vizibile în documentul ownerului.

| Ordine | Feedback / owner | Scor /100 | Lacuna care decide ordinea |
|---:|---|---:|---|
| 1 | [Consumer — refactorizare E2E](ATF_Consumer_Business_Test_Focus_Feedback.md) | **88** = 36 + 24 + 20 + 8 | flow-uri din afara pilotului (88), corelare UI/transport AutoEx (78), runtime Shop (77), declarații repetitive (67), repetare live (57) |
| 2 | [Reporting](ATF_Reporting_Feedback.md) | **77** = 33 + 24 + 16 + 4 | nesting ilizibil și în API nominal, inclusiv Technical trace |
| 3 | [ATF UI — geometrie browser](ATF_UI_Browser_Geometry_Feedback.md) | **72** = 31 + 25 + 10 + 6 | pointer în iframe la zoom CSS non-nativ; activarea JavaScript explicită nu închide această limită |
| 4 | [Shop API — diagnostic](ATF_Practice_Shop_API_Test_Design_Feedback.md) | **66** = 25 + 19 + 16 + 6 | timeout înainte de login fără artifact FAIL; lizibilitatea comună este urmărită la Reporting |
| 5 | [Documentație — ancore](#doc-link-anchor-01--validarea-automată-a-ancorelor) | **44** = 14 + 11 + 15 + 4 | guard-ul validează fișierul local, dar elimină fragmentul înaintea verificării; ancora lipsă nu este detectată |

Ordinea nu înlocuiește criteriul de închidere. O componentă verde nu certifică
un flow remote, iar succesul live nu certifică toate politicile sau randarea.
Calitatea editorială se evaluează separat prin
[rubrica documentației](../README_Atf_DocumentationBestPractices.md#rubrica-reviziei-editoriale).

## Design AI — evaluare separată

[Feedback-ul AI](README_AI_Evaluation.md) păstrează gap-urile evaluatorului și
ale workflow-ului propus. Scorurile sale măsoară readiness-ul, nu prioritatea
operațională de mai sus; nu sunt recalibrate printr-o revizie editorială.

## DOC-LINK-ANCHOR-01 — validarea automată a ancorelor

**Owner:** guvernanța documentației din `atf-tests`. Linkurile curente sunt
corectate, dar `DocumentationReadmeStructureGovernanceTest.inspectLocalLinks`
verifică numai calea dinaintea lui `#`; nu protejează heading-ul destinație.

- [ ] Adăugați verificarea fragmentelor Markdown, inclusiv link local, cross-file,
      Unicode, heading duplicat și ancoră HTML explicită; excludeți exemplele din fences.
- [ ] Probați destinația validă și ancora dispărută după promovarea feedback-ului,
      cu diagnostic fișier/linie/remediere; păstrați controalele de testChanges și
      rulați validatorul și entry point-ul documentației, fără relaxarea regulilor.

## Păstrare, promovare și verificare

- Un gap dispare numai după verificarea criteriului la owner; informația stabilă
  se mută în documentația proiectului, fără istoric de artifacte.
- Un feedback fără gap-uri se șterge numai după reconcilierea controalelor
  `testChanges`, a linkurilor și a țintelor `@DocumentationTarget`.
- README-urile de modul rămân documentație de utilizare chiar fără ToDo-uri.
  Controalele tehnice nu sunt cerințe funcționale și nu se închid editorial.

Protocolul este în [bunele practici documentare](../README_Atf_DocumentationBestPractices.md#păstrarea-și-ștergerea-feedback-ului).
Controalele verificate aparțin [baseline-ului testelor](../ATF_Test_Changes_Baseline.md);
execuțiile respectă [staging-ul și sync-ul sigur](../ATF_Build_And_Execution_Safety.md).
