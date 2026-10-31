# ATF Validation — verdicturi JSON fără fals PASS

## Pe scurt

J0 deține validarea comună. J3 consumă o copie de source identică pentru
clasele afectate; nu folosește acest validator pentru WER sau parserul Whisper.
Un candidat mai strict există în J2 și trebuie reconciliat la owner, apoi
verificat distinct în consumeri. Nu declarăm acel candidat livrat în J0/J3.

## VALIDATION-STRICT-93 — JSON complet, missing distinct și subset de schemă explicit

**Prioritate:** 96/100 = impact 40 + lacună 28 + lucrabil 20 + documentare 8.
**Owner:** `atf-validation` la J0; J2 furnizează candidatul, J1/J2/J3 acceptanțe separate.

Probele externe pe sursa J3, identică cu J0 la aceste clase, au reprodus:

| Trigger | Comportament observat | Comportament cerut |
|---|---|---|
| `{"id":1} broken` | `validJson` acceptă primul obiect | întregul document trebuie consumat |
| `jsonPathAbsent("$.value")` pe `not JSON` | PASS | JSON invalid nu dovedește absența |
| `jsonPathType("$.missing", "null")` pe `{}` | PASS | missing și explicit null sunt distincte |
| `additionalProperties:false` fără `properties`, input cu un câmp | valid | proprietatea suplimentară este respinsă |
| keyword `const:42`, input `99` | valid prin ignorare | keyword suportat corect sau refuz explicit al subsetului |

Sursele sunt [parserul](../../atf-validation/src/main/java/atf/validation/contract/JsonPathSupport.java),
[regulile](../../atf-validation/src/main/java/atf/validation/contract/ResponseContractRuleSet.java)
și [schema](../../atf-validation/src/main/java/atf/validation/contract/JsonSchemaValidator.java).
Un subset de JSON Schema este acceptabil; ignorarea tăcută nu certifică un contract.

- [ ] Reconcilează selectiv candidatul J2 `contract/json` și `contract/schema`
  cu sursa și schimbările live J0; păstrează Git-ul, POM-urile și controalele
  fiecărui deployment. Nu copia întregul modul ori directoarele de build.
- [ ] Păstrează rezultat typed pentru document invalid, missing, null și value;
  consumă întregul JSON și definește politica duplicatelor.
- [ ] Validează capabilitățile/forma schemei înaintea instanței; keywords și
  forme nesuportate sunt refuzate explicit, inclusiv în ramuri neatinse de input.
- [ ] Adaugă regresii permanente pentru cele cinci trigger-e, strict/warning,
  numere și forme de schemă invalide. Păstrează expected/actual integral la
  evaluare, preview 240 și publicarea neutră a assertions.
- [ ] Rulează nativele la owner și acceptanțele J1/J2/J3 pe sursa/binarul
  efectiv distribuit. Candidatul J2 și cele 52 de regresii vechi verzi nu închid
  automat această reconciliere.

**Închidere:** niciunul dintre trigger-e nu mai produce valid/PASS incorect;
contractul subsetului este în README-ul Validation, probele permanente sunt
verzi și consumerii au acceptanțe proprii. Catalogul J3 rămâne de opt module,
Core C1 și Reporting J7 nu cer schimbare doar pentru acest fix de Validation.

[Owner Validation](../../atf-validation/README.md) ·
[sincronizare](../ATF_Workspace_Synchronization.md) · [registru](README.md).
