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
- Etapele purchase AutoEx și gate-urile catalogului Shop au owner în main; rămân
  contextul E2E Shop și billing în suita Shop.
- Checkout-ul AutoEx așteaptă vizibilitatea înaintea clickului unic; rămân
  precondițiile business și recuperarea BE propusă, fără replay de mutații.
- Componentele Shop/AutoEx au un raport per clasă/suită și audit per invocare;
  guvernanța folosește selectorii executabili, cu comentariile excluse.
- Preview-ul E2E și Fails păstrează limita integrală după redactare și contextul
  acțiunii înaintea headline-ului; durata măsurată a componentelor exclude publicarea.
- Căutarea AutoEx confirmă query-ul și produsul vizibil după un click unic,
  cu același buget și fără așteptarea navigărilor fără legătură.
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
| 1 | [UI — sincronizare și recuperare BE](ATF_UI_Backend_Guided_Recovery_Feedback.md) | **80** = 33 + 24 + 16 + 7 | politică YAML/adnotare (80), precondiții checkout (79), audit (78), select înlocuit (74) |
| 2 | [Consumer — runtime și validare](ATF_Consumer_Business_Test_Focus_Feedback.md) | **77** = 31 + 18 + 20 + 8 | context E2E Shop (77), billing în suita Shop (71), evaluator pur (63), echivalența profilelor (57) |
| 3 | [ATF UI — geometrie browser](ATF_UI_Browser_Geometry_Feedback.md) | **72** = 31 + 25 + 10 + 6 | pointer în iframe la zoom CSS non-nativ; activarea JavaScript explicită nu închide limita |

Ordinea nu înlocuiește criteriul de închidere. O componentă verde nu certifică
un flow remote, iar succesul live nu certifică toate politicile sau randarea.
Calitatea editorială se evaluează separat prin
[rubrica documentației](../README_Atf_DocumentationBestPractices.md#rubrica-reviziei-editoriale).

Controlul tehnic al [scope-ului componentelor](ATF_Component_Report_Scope_Feedback.md#controlul-incrementului)
rămâne pentru verificarea Git; nu reprezintă o lacună funcțională activă.
Aceeași separare se aplică [controalelor Reporting](ATF_Reporting_Feedback.md#controale-tehnice-încă-necesare).

## Design AI — evaluare separată

[Feedback-ul AI](README_AI_Evaluation.md) păstrează gap-urile evaluatorului și
ale workflow-ului propus. Scorurile sale măsoară readiness-ul, nu prioritatea
operațională de mai sus; nu sunt recalibrate printr-o revizie editorială.

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

<details>
<summary><strong>Control tehnic — ancorele documentației</strong></summary>

```yaml
testChanges:
  - id: DOC-LINK-ANCHOR-01
    status: ACTIVE
    base: 3219ef1ff546369d00a006cfc7434644251c6f9a
    expires: 2026-11-26
    reason: Întărește rezolvarea linkurilor README fără relaxarea regulilor existente; adaugă probe negative pentru fragment dispărut, Unicode, duplicate și exemple fenced.
    proof: mvn -pl atf-tests -am "-Dtest=DocumentationAnchorGovernanceTest,DocumentationTargetGovernanceTest,WholeAtfGovernanceSuiteTest" "-Datf.governance.testChanges.baseRef=3219ef1ff546369d00a006cfc7434644251c6f9a" "-Dsurefire.failIfNoSpecifiedTests=false" test
    paths:
      - atf-tests/src/test/java/atf/api/tests/governance/documentation/DocumentationReadmeStructureGovernanceTest.java
      - atf-tests/src/test/java/atf/api/tests/governance/documentation/DocumentationAnchorGovernanceTest.java
      - atf-tests/src/test/java/atf/api/tests/governance/documentation/links/MarkdownAnchorIndex.java
```

</details>
