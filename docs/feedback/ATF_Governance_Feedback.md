# ATF Tests — inventarul fidel al surselor compilate

## Pe scurt

Inventarul Maven este comun guard-urilor de nivel și lizibilitate. Criteriul
de mai jos separă output-ul generat de un nume legal de package în sursa declarată.

## GOV-SOURCE-EXCLUSIONS-72 — output versus sursă declarată

**Prioritate: 70/100** = impact 27 + lacună 20 + lucrabil 17 + documentare 6.
**Owner:** ATF Tests, suportul inventarului. **Nivel:** componentă filesystem și guard-uri negative.

Proba creează un POM standard și patru clase compilabile. Inventarul întoarce
numai două: omite clasele din package-urile `legal.target` și `legal.artifacts`,
deși acestea sunt sub rădăcinile main/test declarate de convenția Maven.

| Sursă | Observație |
|---|---|
| [AtfJavaSourceInventory](../../atf-tests/src/test/java/atf/api/tests/governance/layers/AtfJavaSourceInventory.java) | excluderea se aplică fiecărui segment, inclusiv în sursa Java |
| [oracolul existent](../../atf-tests/src/test/java/atf/api/tests/governance/AtfSourceReadabilityGovernanceTest.java) | exclude explicit și `shop/src/main/target/Generated.java`; problema cere reconcilierea criteriului, nu o ștergere ad-hoc a probei |
| parcurgerea root-ului | `Files.walk` intră și în output-urile eliminate ulterior de filtru; observație statică de eficiență, fără SLA nou |

- [ ] Delimitați rădăcinile de output de package-urile compilabile; păstrați ambele layout-uri Shop, default-urile Maven, overlap-ul și ordinea deterministă.
- [ ] Probați că o încălcare reală din package-ul legal `target` sau `artifacts` este detectată de guard-ul afectat, iar output-ul generat rămâne exclus.
- [ ] Reconciliați explicit oracolul vechi prin controlul testChanges potrivit; nu reduceți selecțiile, pragurile ori controalele source/binary pentru verde.
- [ ] Evitați traversarea output-urilor cunoscute înainte de filtrarea fișierelor; orice reutilizare de snapshot rămâne scoped investigației, fără cache global stale între fixture-uri.

**Închidere:** setul surselor compilabile și setul inspectat coincid în fixture-ul
nominal, iar mutanții sunt refuzați în ambele deployment-uri. XML nesigur,
rădăcinile ieșite din modul și expresiile nerezolvate rămân refuzate.

## GOV-PRODUCER-CHANGE-CONTROLS-73 — reconcilierea bazei producătorului

**Prioritate: 64/100** = impact 24 + lacună 20 + lucrabil 13 + documentare 7.
**Owner:** integratorul `demo2` și ownerii schimbărilor de teste.
**Nivel:** control Git pe snapshot-ul sursă și guvernanță whole-ATF.

La baza `4f6bd353bf23f499a1268e811116b5627522306c`, controlul refuză diferențe
preexistente de teste/POM fără înregistrare eligibilă. Perimetrul include
Reporting, Reporting Tests și infrastructura comună; proba facturii AutoEx are
control propriu și nu autorizează aceste diferențe. Hash-urile ori absențele
din snapshot delimitează lucrul preexistent de incrementul curent.

- [ ] Inventariați fiecare path și statut Git față de baza reală a revizuirii; păstrați separat sursele, indexul live și snapshot-ul TMP.
- [ ] Revizuiți motivul și proba fiecărei schimbări; declarați un singur control eligibil per path/bază, cu expirare și `allowDelete` numai pentru ștergeri revizuite.
- [ ] Marcați `VERIFIED` numai incrementul demonstrat; nu rebazați automat controale istorice și nu sintetizați aprobări generale pentru a obține verde.
- [ ] Reluați controlul Git și suita canonică pe snapshot-ul reconciliat, fără relaxarea filtrelor, a selecțiilor ori a politicii.

**Închidere:** [TestChangeGovernanceTest](../../atf-tests/src/test/java/atf/api/tests/governance/changes/TestChangeGovernanceTest.java)
și suita whole-ATF trec cu baza explicită și sursele livrate. J1 păstrează
evaluarea propriei baze; criteriul producătorului nu se copiază ca defect binar.
Protocolul rămâne în [baseline](../ATF_Test_Changes_Baseline.md) și în
[procesul de staging](../ATF_Build_And_Execution_Safety.md#staging-pentru-guvernanța-schimbării-testelor).
