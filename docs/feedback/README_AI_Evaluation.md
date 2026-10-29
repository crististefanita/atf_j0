# ATF Java — evaluarea AI față de codul curent

## Pe scurt

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

Scorurile de mai jos sunt calibrări motivate de readiness, nu quality score
al unui model și nici prioritățile registrului central. Contractul implementat
rămâne la owner; designul propus nu este probă de livrare.

### Scorecard executiv

| Axă | Scor | Ce dovedește scorul | Limita actuală |
|---|---:|---|---|
| Completitudinea designului de referință | **94/100** | contracte typed, expected/observed, evidence, grounding, review/state, score, hard-fail, snapshot, regression și workflow agentic | designul nu este încă executabil ca vertical slice |
| Fundamentul ATF Java pentru evidence/reporting | **87/100** | request/response, retry history, provenance, auth observations, semantic snapshot și renderer-neutral projection | vocabularul este generic, nu AI-specific |
| Validator generic reutilizabil | **66/100** | `ValidationEvaluator`, `ValidationTable`, validatori de contract API și comparator AI tipat | evaluatorul AI rămâne separat de rubricile business și de reporting |
| AI evaluation implementată în ATF Java | **50/100** | comparatorul, hard-fails, invocation evidence, grounding per field, snapshot-ul `COMPLETE` și package cohesion sunt executabile și testate | clasificarea `AMBIGUOUS`, provider real, evidence ledger și proiecție raport lipsesc |
| AI-assisted workflow governance | **31/100** | guvernanță whole-ATF și scope-uri `testChanges` cu Git, expirare, ownership unic și probe negative | lipsesc expected delta tipizat, proof ledger și handoff; acoperirea generală nu certifică workflow-ul agentic sau AI quality |
| Pregătirea compoziției agentice | **25/100** | responsabilitățile sunt delimitate la ownerii existenți | lipsesc contractele runtime și controalele; numărul modulelor nu este criteriu de readiness |
| Potrivirea principiilor ATF1 pentru adoptare | **86/100** | cold baseline, claim → proof → artifact, handoff, anti-copy și separarea orchestrator/execution/reporting sunt direct transferabile | nu trebuie importată economia de runs/slots ca runtime ATF |
| Potrivirea pattern-urilor .NET pentru adoptare | **92/100** | read models typed, field decisions, retry contract, evidence policy, auth evidence și ownership unic | modelele sunt business-specific și trebuie transpuse, nu copiate |
| Readiness pentru primul pilot AI | **78/100** | evaluatorul typed, snapshot complet, grounding allowlisted, matricea missing/invented și contractul invocation/fallback permit un pilot fără provider integrat | lipsesc publication path, run ledger și cazuri business negative |

**Concluzie:** ATF Java are un evaluator deterministic cu invocation provenance,
dar nu un vertical slice AI end-to-end: codul nu invocă modelul, nu dovedește
grounding la nivel de run și nu publică un snapshot AI în raport. Un raport Extent
frumos sau transportul `LIVE_HTTP` nu demonstrează evaluare AI.

<details>
<summary>Corespondența cerință–cod și dovezile ownerilor</summary>

### Cerință față de codul curent

| Cerință țintă | Cod observat live | Score |
|---|---|---:|
| expected separat de observed | `AiEvaluationCase` versionat și immutable se construiește separat de harta observed | **65/100** |
| field-level typed comparison | opt moduri sigure, type/reason codes, weight, critical, source presence și output extra detectat implicit; regex restrâns este deferred | **86/100** |
| missing information și non-invention | missing/invented critical, source/expected contradiction și undeclared output au probe negative; grounding-ul obligatoriu este evaluat separat și produce hard-fail când reference-ul lipsește ori nu este permis | **86/100** |
| grounding și source references | allowlist per field, reference opac și probe `SUPPORTED` / `UNSUPPORTED` / `MISSING_REFERENCE` sunt implementate; `AMBIGUOUS` există numai ca valoare enum, fără clasificare în evaluator sau test; run ledger lipsește | **58/100** |
| review și state transitions | există lifecycle/report status și consumeri UI, nu contract AI pentru review obligatoriu și tranziții permise | **22/100** |
| invocation mode și fallback | `AiInvocationEvidence` distinge `REAL_AI`, `MOCK_MODEL`, `EXPLICIT_FALLBACK`, `UNKNOWN`; numai real cu references complete poate primi quality score | **74/100** |
| score și hard-fail precedence | threshold, mismatch critic, missing/invented/undeclared rule IDs și `FAIL` independent de score sunt testate | **74/100** |
| snapshot semantic renderer-neutral | `AiEvaluationSnapshot` păstrează rezultatul immutable, policy, completion time, grounding observations și invocation; evidence index și run-level trace încă lipsesc | **86/100** |
| lifecycle și checkpoint unic | UI/API au phase lifecycle și evidence publisher; AI validation nu este încă un sibling semantic real al UI/API | **36/100** |
| Extent/fallback parity | există semantic snapshot comun pentru raportare și fallback; nu există proiecție AI | **52/100** |
| regression semantic delta | există snapshot și audit de raport, nu comparator AI pentru changed fields/grounding/review/invocation | **26/100** |
| governance transversală | `WholeAtfGovernanceSuiteTest` acoperă boundary, ownership, coeziune, readability, documentație și politica schimbării testelor; scorul măsoară acoperirea, nu faptul că execuția curentă este verde | **82/100** |
| governance pentru agent handoff | nu există încă proposal bounded, proof plan, machine-readable handoff sau compiled feedback | **18/100** |

### Dovezi concrete din ATF Java

Contractele deja implementate au documentație oficială; acest feedback nu
menține un al doilea inventar al claselor ori al probelor lor.

| Contract existent | Sursa normativă |
|---|---|
| Comparator AI, invocation, grounding, snapshot și organizarea pachetelor | [ATF Validation](../../atf-validation/README.md#comparatorul-ai--contract-implementat) |
| Request/response, attempts/retry, provenance și semantic snapshot | [ATF Reporting](../../atf-reporting/README.md), [contractul de raportare](../ATF_Reporting_Contract.md) |
| Observația bounded/redacted a traficului și politica fără replay | [ATF UI](../../atf-ui/README.md) |
| Guvernanță transversală și probele modulelor | [ATF Tests](../../atf-tests/README.md), [ghidul de testare](../ATF_Testing_Guide.md) |

Aceste capabilități oferă infrastructura pilotului; nu demonstrează încă
invocarea unui provider AI, registrul de dovezi la nivel de run sau proiecția AI.

</details>

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

Scorul curent al implementării este **50/100**: comparatorul și snapshot-ul
typed există, dar ambiguitatea grounding-ului, run ledger-ul, integrarea
workflow-ului și proiecția de raport rămân nedemonstrate. Guvernanța generală
și inspectorul de rețea nu certifică AI quality. Ordinea domeniilor este în
[registrul feedback](README.md), iar contractele și opțiunile proiectate sunt
în [designul AI](../ATF_AI_Evaluation_Design.md).
