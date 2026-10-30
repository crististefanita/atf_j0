# ATF Java — evaluarea AI față de codul curent

## Pe scurt

**Trasee rapide:** [livrat versus propus](../../atf-validation/README.md#comparatorul-ai--contract-implementat) ·
[gap-uri](#gap-uri-rămase-pentru-evaluator) ·
[readiness](#readiness-față-de-codul-curent) · [probe](#verificarea-comparatorului-livrat)

Acest document separă gap-urile evaluatorului AI de readiness-ul implementării.
Designul țintă și pattern-urile extrase din ATF1/.NET sunt în
[designul AI al proiectului](../ATF_AI_Evaluation_Design.md).

Backlog-ul AI nu intră în ordinea priorităților active din registrul comun.
Implementarea propusă păstrează cele 12 module; absența unui modul nou nu
constituie gap sau penalizare.

| Stare | Owner / limită |
|---|---|
| Implementat și testat | [Comparatorul AI din ATF Validation](../../atf-validation/README.md#comparatorul-ai--contract-implementat): expected separat, comparații typed, hard-fail, invocation evidence, grounding allowlisted și snapshot immutable |
| Parțial | Guvernanța whole-ATF și scope-urile `testChanges`; nu există încă proposal/handoff complet |
| Deschis | Ambiguitate grounding, run ledger, lifecycle, review/state, rubrică business, raportare și regresii |
| Doar design | compoziție agentică în modulele existente, guard-uri și YAML conceptual; nu sunt opțiuni livrate |

O implementare se închide numai cu probă nominală și negativă la boundary-ul
potrivit. Un document mutat, o clasă nouă sau un raport frumos nu ridică scorul
de implementare. Modificarea testelor de guvernanță urmează regula ownerului din
[README-ul ATF Tests](../../atf-tests/README.md#guvernanța-schimbării-testelor).

## Gap-uri rămase pentru evaluator

| ID | Cerință lipsă | Criteriu de închidere |
|---|---|---|
| `AI-EVAL-04` | clasificarea grounding-ului ambiguu | evaluatorul produce `AMBIGUOUS` cu motiv și referință; un caz negativ verifică verdictul și absența promovării la `SUPPORTED` |
| `AI-EVAL-05` | review/state | acțiunea și tranzițiile sunt evaluate separat, inclusiv review lipsă și tranziție interzisă |
| `AI-EVAL-06` | integrarea invocation/fallback în lifecycle | run-ul corelează modul real/mock/fallback, output-ul și probele; lipsa ori clasificarea greșită a dovezii eșuează explicit |
| `AI-EVAL-07` | catalog AI business și rubrică pe caz | reguli versionate și cazuri negative pentru intent/review/state; hard-fail prevalează indiferent de scor |
| `AI-EVAL-08` | registru de dovezi la nivel de run | index/correlation/trace leagă invocation, grounding și snapshot; reference lipsă sau străină este respinsă |
| `AI-EVAL-09` | publicare și paritate de raportare | snapshot-ul AI este publicat fidel în Extent/fallback; testele compară aceeași semantică și diagnosticul de mismatch |
| `AI-EVAL-10` | regression delta | comparator între snapshot-uri versionate cu cazuri new/fixed/changed și baseline incompatibil |
| `AI-EVAL-11` | coverage governance AI | fiecare contract public nou are probă nominală și negativă; guard-ul identifică o probă eliminată |
| `AI-EVAL-12` | UI proof | browserul verifică review/state/evidence vizibile, corelate cu snapshot-ul și fără propoziții duplicate |

## Gap-uri de design pentru workflow-ul agentic

| ID | Gap | Prioritate | Dovadă necesară |
|---|---|---:|---|
| `AI-WORKFLOW-01` | nu există change proposal bounded pentru agent | P1 | proposal valid/invalid și diagnostics |
| `AI-WORKFLOW-02` | handoff-ul nu este machine-readable | P1 | handoff complet cu diff/gates/evidence |
| `AI-WORKFLOW-03` | lipsește validarea owner/layer/proof plan pe proposal-ul agentic | P1 | governance tests fail-closed, distincte de inventarul general al modulelor |
| `AI-WORKFLOW-04` | agentul nu are feedback compilat standard | P1 | feedback cu rule, fact, fix și next proof |
| `AI-WORKFLOW-05` | impactul documentar al agent-delta nu are guard executabil | P2 | documentation drift proof |
| `AI-WORKFLOW-06` | separarea scorurilor este proiectată, dar nu are două snapshot-uri runtime | P1 | workflow score și AI quality au verdicte distincte, fără calcul din textul agentului |
| `AI-WORKFLOW-07` | pretraining-ul este doar disciplină, nu contract | P2 | cold baseline card bounded |
| `AI-EVAL-13` | test generated nu este verificat pentru observed facts | P1 | literal verdict guard + evidence binding |
| `AI-EVAL-15` | feedback-ul de regression nu este handoff pentru agent | P2 | semantic delta → next proof |

Aceste cerințe descriu un design viitor, nu un modul sau o opțiune livrată.
Orice implementare rămâne o schimbare separată, cu owner, boundary și probe
nominale/negative proprii.

## Readiness față de codul curent

Evaluarea de mai jos privește readiness-ul implementării, nu quality score
al unui model și nici prioritățile registrului central. Contractul implementat
rămâne la owner; designul propus nu este probă de livrare.

### Scorecard executiv

Implementarea rămâne **50/100**, conform evaluării existente: comparatorul,
invocation provenance și snapshot-ul typed sunt livrate; clasificarea
`AMBIGUOUS`, registrul de run, lifecycle-ul AI și proiecția raportului lipsesc.
Această curățare editorială nu recalculează readiness-ul și nu închide cerințe.

Contractul executabil este la [Validation](../../atf-validation/README.md#comparatorul-ai--contract-implementat);
infrastructura generică de [raportare](../ATF_Reporting_Contract.md),
[observație UI](../../atf-ui/README.md) și
[guvernanță](../../atf-tests/README.md) nu dovedește singură o integrare AI.
Tabelele de gap-uri de mai sus păstrează ID-ul, boundary-ul și proba lipsă;
designul și opțiunile conceptuale rămân în documentul de proiect.

### Plan de adoptare în ordine corectă

```text
1. AMBIGUOUS produs de evaluator + probă negativă
2. registru de dovezi la nivel de run + corelarea lifecycle-ului
3. catalog business versionat: intent, review/state și hard-fail
4. checkpoint semantic UI/API/AI + publicare și paritate Extent/fallback
5. regression delta + guard-uri de proposal/handoff
```

Primul pilot folosește comparatorul existent, un caz cu input controlat și
două output-uri negative. Dovada minimă este component/integration; browserul
verifică ulterior efectul observabil. Providerul real are nevoie de propria
probă de integrare și nu înlocuiește matricea deterministă.

## Criteriu de închidere și recalibrare

- `AMBIGUOUS` trebuie produs de evaluator și probat negativ, nu doar declarat în enum.
- Registrul de run leagă invocation, output, grounding și snapshot; referințele lipsă sau străine eșuează explicit.
- Publicarea proiectează snapshot-ul AI fără să recalculeze verdictul și păstrează paritatea Extent/fallback.
- Cazurile versionate verifică intent, review/state și hard-fail; providerul real are propria probă de integrare.
- Workflow-ul agentic are proposal/handoff valid și invalid; nu își modifică singur gate-urile.
- La închidere, contractul demonstrat se promovează în README-ul ownerului și dispare din acest backlog.

Ordinea domeniilor este în [registrul feedback](README.md), iar contractele și
opțiunile proiectate sunt în [designul AI](../ATF_AI_Evaluation_Design.md).

## Verificarea comparatorului livrat

| Întrebare | Sursă și probă | Limită |
|---|---|---|
| Expected-ul și verdictul sunt independente de renderer? | [evaluator](../../atf-validation/src/main/java/atf/validation/ai/AiEvaluationEvaluator.java) și [AiEvaluationEvaluatorTest](../../atf-validation/src/test/java/atf/validation/ai/AiEvaluationEvaluatorTest.java) | comparație locală; nu invocă provider AI |
| Ce este publicat de modul? | [Contractul Validation](../../atf-validation/README.md#comparatorul-ai--contract-implementat) și [proba locală](../../atf-validation/README.md#verificare-rapidă) | snapshot-ul nu este încă o proiecție AI în Extent |
| Ce rămâne numai design? | [Design AI](../ATF_AI_Evaluation_Design.md) și criteriile din acest feedback | niciun YAML conceptual nu devine opțiune runtime prin documentare |

Readiness-ul existent nu se recalculează printr-o îmbunătățire editorială.
Calitatea acestui README și completitudinea funcționalității propuse sunt
evaluări distincte, conform [rubricii documentare](../README_Atf_DocumentationBestPractices.md#rubrica-reviziei-editoriale).
