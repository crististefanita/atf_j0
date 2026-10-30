# ATF — fidelitatea contractelor documentate

## Pe scurt

Guvernanța verifică forma, diagramele, inventarul și linkurile documentației.
Afirmațiile factuale critice cer și comparație cu ownerul executabil; o notă
editorială sau un gate structural verde nu certifică singur această fidelitate.

## DOC-FACTUAL-CONTRACT-35 — probă semantică pentru afirmațiile critice

**Prioritate: 63/100** = impact 23 + lacună 17 + lucrabil 17 + documentare 6.
**Owner:** documentul contractului; ATF Tests pentru o probă neutră ulterioară.

README-ul Reporting eticheta drept `Boundary permis` o coloană care enumera
module ATF, browser și fixture-uri de business pentru `src/main`. Introducerea,
POM-ul standalone și codul le exclud; formularea a fost corectată la owner.
Restanța este detectarea unei asemenea contradicții, nu încă o regulă de stil.

| Boundary inspectat | Ce demonstrează și ce rămâne manual |
|---|---|
| `DocumentationStyleGovernanceTest` și `DocumentationReadmeStructureGovernanceTest` | stil, structură și linkuri; nu sensul unei coloane de responsabilități |
| `DocumentationAnchorGovernanceTest` | ținta și fragmentul există; propoziția de lângă link poate rămâne falsă |
| POM, settings și enum-uri la owner | sursa executabilă pentru dependențe, scope și default-uri; trebuie comparată cu afirmația documentată |

- [ ] Delimitați un set mic de afirmații verificabile: cele 12 module, scope-ul
      Kit al consumerilor, izolarea standalone Reporting și default-ul preview 240.
      Nu construiți un parser general de proză sau un al doilea registru de adevăr.
- [ ] Legați afirmația de sursa executabilă și owner, cu diagnosticul document,
      fragment, valoare declarată și valoare observată. Exemplele configurabile,
      precum preview 18 în probe, rămân distincte de default-ul produsului.
- [ ] Proba negativă schimbă numai afirmația documentată: Kit `test` devine
      `compile`, numărul modulelor devine 13 sau default-ul este greșit. Oracolul
      trebuie să refuze contradicția cu source/POM neschimbate, fără expected
      calculat din aceeași propoziție pe care încearcă să o verifice.
- [ ] Ambiguitățile de limbaj și contradicțiile care nu au reprezentare typed
      rămân criteriu de review manual. Nu declarați verificarea semantică completă
      fiindcă au trecut regex-uri, ancore sau un eșantion de contracte.
- [ ] Păstrați rubrica editorială, diagramele și contractele stabile la owner;
      punctajele se raportează în canal. Nu modificați ori relaxați gate-urile
      existente pentru a accepta o afirmație falsă.

**Închidere:** mutațiile factuale greșite sunt detectate independent, cu limitele
automatizării declarate; documentația corectă și probele structurale rămân verzi.

Regulile stabile sunt în [ghidul documentației](../README_Atf_DocumentationBestPractices.md#rubrica-reviziei-editoriale),
iar verificarea urmează [procesul izolat](../ATF_Build_And_Execution_Safety.md).
