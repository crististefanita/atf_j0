# ATF Java — registrul cerințelor rămase

Registrul indexează numai gap-uri active. Contractele stabile aparțin README-ului
ownerului; rapoartele și rezultatele unei rulări nu devin documentație permanentă.

## Pe scurt

- Reporting: lizibilitatea tabelelor imbricate în panouri înguste rămâne
  deschisă. Flow-ul UI-only cu un singur set de tabele și corelarea eșecurilor
  sunt livrate la [owner](../../atf-reporting/README.md).

- AutoEx: contractul livrat pentru Delete account, Close JavaScript la 70% și
  separarea navigării de traficul browser este în [README-ul modulului](../../atf-test-auto-ex/README.md#delete-account--navigare-întreruptă).
  Activarea DOM Close nu repară geometria pointerului; limita generică rămâne deschisă.
- Consumeri: scope-ul contului și runtime-ul main AutoEx sunt livrate.
  Separarea business/UI/validare, contextul Shop și configurația completă de
  evidence rămân deschise; reducerea selecției live cere echivalență demonstrată.
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
| 1 | [Consumer — refactorizare E2E](ATF_Consumer_Business_Test_Focus_Feedback.md) | **88** = 36 + 24 + 20 + 8 | separare business/UI/validare (88), evidence și Overview fidel (80), corelare UI/transport AutoEx (78), runtime Shop (77), adnotări compuse (67), repetare live (57) |
| 2 | [Guvernanță — namespace-uri](#gov-namespace-match-02--probe-negative-pentru-boundary) | **86** = 35 + 25 + 20 + 6 | matcher-ul schimbat cere probe negative, distincte de scanarea unui checkout conform |
| 3 | [Reporting](ATF_Reporting_Feedback.md) | **77** = 33 + 24 + 16 + 4 | nesting ilizibil și în API nominal, inclusiv Technical trace (77); launcher browser canonic (58) |
| 4 | [ATF UI — geometrie browser](ATF_UI_Browser_Geometry_Feedback.md) | **72** = 31 + 25 + 10 + 6 | pointer în iframe la zoom CSS non-nativ; activarea JavaScript explicită nu închide această limită |
| 5 | [Shop API — diagnostic](ATF_Practice_Shop_API_Test_Design_Feedback.md) | **66** = 25 + 19 + 16 + 6 | timeout înainte de login fără artifact FAIL; lizibilitatea comună este urmărită la Reporting |
| 6 | [Documentație — ancore](#doc-link-anchor-01--validarea-automată-a-ancorelor) | **44** = 14 + 11 + 15 + 4 | guard-ul validează fișierul local, dar elimină fragmentul înaintea verificării; ancora lipsă nu este detectată |


Ordinea nu înlocuiește criteriul de închidere. O componentă verde nu certifică
un flow remote, iar succesul live nu certifică toate politicile sau randarea.
Calitatea editorială se evaluează separat prin
[rubrica documentației](../README_Atf_DocumentationBestPractices.md#rubrica-reviziei-editoriale).

## Design AI — evaluare separată

[Feedback-ul AI](README_AI_Evaluation.md) păstrează gap-urile evaluatorului și
ale workflow-ului propus. Scorurile sale măsoară readiness-ul, nu prioritatea
operațională de mai sus; nu sunt recalibrate printr-o revizie editorială.

## GOV-NAMESPACE-MATCH-02 — probe negative pentru boundary

**Owner:** `atf-tests` pentru matcher-ul de dependențe și Reporting Tests pentru
guard-ul API/UI. Refactorizarea folosește acum potrivire pe package/import/FQCN;
scanarea unui checkout conform nu dovedește că o dependență interzisă este respinsă.

- [ ] Probați cu surse controlate import normal/static/wildcard, declarație de
      package și tip complet calificat în corpul metodei, inclusiv încălcarea
      introdusă deliberat; afirmați fișierul și regula din diagnostic.
- [ ] Distingeți namespace-ul interzis de un prefix asemănător permis și de
      chei de configurare în string-uri; declarați tratamentul comentariilor și
      verificați că exemplele nu maschează o referință Java reală.
- [ ] Verificați maparea vechi → nou a fișierelor mutate cu aceleași cazuri de
      boundary. Schimbarea matcher-ului și a testelor păstrează controale exacte,
      fără rebazare sau allowlist de evitare.

Contractele stabile sunt în [arhitectură](../ATF_Architecture.md#namespace-uri-și-rădăcini-de-surse)
și [README-ul ATF Tests](../../atf-tests/README.md#guvernanța-schimbării-testelor).

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
