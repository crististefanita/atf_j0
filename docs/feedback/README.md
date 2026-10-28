# ATF Java — registrul cerințelor rămase

Registrul indexează numai gap-uri active. Contractele stabile aparțin README-ului
ownerului; rapoartele și rezultatele unei rulări nu devin documentație permanentă.

## Pe scurt

- Componentele au evaluare JUnit unică, dovezi redactate înaintea plafonului,
  faze lifecycle și rezultate reconciliate; contractele sunt la ownerii Kit și Reporting.
- API Shop deține auditul înaintea execuției și păstrează artifactul pe ieșirile
  de eșec; runtime-ul și scope-ul coșului sunt în main.
- AutoEx păstrează un raport API comun în suită și raport propriu per clasă;
  cleanup-ul poate reveni prin GET din documentul queue full, fără replay de payment.
- Rămân separarea flow-urilor, contextul E2E Shop, billing în suita Shop
  și lizibilitatea panourilor înguste.
- Contractele stabile sunt în [indexul documentației](../README.md);
  feedback-ul păstrează numai ownerul, reproducerea și criteriul rămas.

## Top feedback-uri active

Scorul este o prioritate de intervenție, nu o notă a codului sau a README-ului.
Formula comună este impact /40 + severitatea lacunei /30 + lucrabil acum /20 +
datorie documentară /10; toate domeniile operaționale au aceeași pondere.

Un feedback cu mai multe gap-uri primește scorul celui mai urgent, nu suma
lor. Gap-urile secundare rămân vizibile în documentul ownerului.

| Ordine | Feedback / owner | Scor /100 | Lacuna care decide ordinea |
|---:|---|---:|---|
| 1 | [Consumer — flow-uri și runtime](ATF_Consumer_Business_Test_Focus_Feedback.md) | **88** = 36 + 24 + 20 + 8 | porturi și etape rămase (88), context E2E Shop (77), billing în suita Shop (71), evaluator pur (63) |
| 2 | [Reporting](ATF_Reporting_Feedback.md) | **77** = 33 + 24 + 16 + 4 | nesting ilizibil în panouri înguste |
| 3 | [ATF UI — geometrie browser](ATF_UI_Browser_Geometry_Feedback.md) | **72** = 31 + 25 + 10 + 6 | pointer în iframe la zoom CSS non-nativ; activarea JavaScript explicită nu închide limita |
| 4 | [Documentație — ancore](#doc-link-anchor-01--validarea-automată-a-ancorelor) | **44** = 14 + 11 + 15 + 4 | guard-ul validează fișierul, dar nu fragmentul Markdown |

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
