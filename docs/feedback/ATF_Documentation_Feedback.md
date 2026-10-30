# ATF — fidelitatea contractelor documentate

## Pe scurt

Forma, diagramele, inventarul și linkurile au guvernanță executabilă.
[Guard-ul factual restrâns](../../atf-tests/README.md#documente-și-responsabilități)
compară declarația celor 12 module și matricea Kit cu POM-urile reale, inclusiv
mutanți care schimbă numai documentul. Acest contract nu certifică sensul întregii proze.

## DOC-REPORTING-FACTUAL-36 — afirmațiile rămase la ownerul Reporting

**Prioritate: 55/100** = impact 20 + lacună 14 + lucrabil 15 + documentare 6.
**Owner:** Reporting/Reporting Tests pentru documente; ATF Tests pentru un guard
neutru numai după delimitarea afirmației și a sursei sale executabile.

| Afirmație rămasă | Sursă și probă necesară |
|---|---|
| README Reporting Tests descrie snapshot-ul ca probă a zoom-ului CSS efectiv | snapshot-ul verifică valori configurate; măsurarea browserului aparține UI și trebuie numită distinct |
| README audit Extent afirmă viewport CSS stabil sub zoom atât în configurarea browserului, cât și în criteriul de geometrie | [contractul UI](../../atf-ui/README.md#geometria-nativă-a-contextului) derivă viewport-ul CSS și DPR; aliniați ambele propoziții, fără schimbarea rendererului |
| izolarea standalone și preview-ul descrise în proză | criteriu de audit semantic, fără defect nou reprodus: confruntați afirmația cu POM/sursa și proba ownerului; default-ul 240 și exemplul configurat 18 au roluri distincte |

- [ ] Reconciliați afirmațiile din [README Reporting Tests](../../atf-reporting-tests/README.md)
      și [README audit Extent](../../atf-reporting-tests/src/test/java/atf/ui/tests/extent/README.md)
      cu distincția configurație publicată / geometrie măsurată / pixeli capturați.
- [ ] Extindeți automatizarea numai pentru declarații stabile, cu diagnostic
      document, fragment, valoare declarată, valoare observată și sursă executabilă.
      Mutantul schimbă documentul, cu sursa neschimbată; fără registru paralel de adevăr.
- [ ] Păstrați review-ul manual pentru ambiguități fără reprezentare typed.
      Regex-urile, ancorele și gate-ul semantic restrâns nu declară fidelitate completă.

Aceste corecții aparțin perimetrului Reporting și rămân separate de incrementul
consumerilor/UI. Nu relaxați gate-urile structurale ori factuale pentru o afirmație falsă.

Corecțiile factuale de geometrie sunt restanțe concrete; extinderea guard-ului
rămâne o îmbunătățire delimitată, fără a cere un parser universal al prozei.

**Închidere:** propozițiile corespund sursei, iar contradicțiile selectate sunt
refuzate independent; limitele automatizării rămân explicite la owner.

Rubrica editorială este în [ghidul documentației](../README_Atf_DocumentationBestPractices.md#rubrica-reviziei-editoriale),
iar verificarea urmează [procesul izolat](../ATF_Build_And_Execution_Safety.md).
