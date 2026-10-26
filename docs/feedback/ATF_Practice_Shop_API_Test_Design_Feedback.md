# Practice Shop API — diagnostic la eșecul transportului

## Pe scurt

Ownerul este adaptorul API standalone Shop. Rămâne raportul FAIL pentru lipsa
răspunsului la login (**66/100**); succesul autentificării browser nu închide
această probă.

## SHOP-API-FAILURE-ARTIFACT-02 — login fără răspuns și fără raport

**Prioritate:** 66/100 = impact 25 + lacună 19 + lucrabil 16 + documentare 6.
[`ShopStandaloneApiReportTest`](../../atf-test-practice-shop/src/test/atf/shop/tests/api/integration/ShopStandaloneApiReportTest.java)
execută login-ul înaintea publicării artifactului.
Un `ApiExecutionException` la această limită poate lăsa numai diagnosticul
Surefire/consolă, fără raportul API de investigat.

- [ ] Publicați un artifact FAIL inclusiv pentru timeout/conexiune eșuată
      înaintea login-ului, cu request redactat și cauza originală.
- [ ] Distingeți răspunsul HTTP respins de absența răspunsului: deadline-ul
      transportului nu dovedește credențiale invalide și nu primește status/body inventat.
- [ ] Probați boundary-ul cu transport controlat și verificați artifactul
      rezultat în browser, înainte de o nouă probă live opt-in.
- [ ] Investigați separat configurația/conectivitatea transportului Java;
      succesul login-ului browser nu certifică login-ul API standalone.

## Limita față de randarea comună

Schema standalone, redactarea și mapping-ul consumerului aparțin
[README-ului Shop](../../atf-test-practice-shop/README.md#boundary-ul-probelor-api).
Lizibilitatea tabelelor imbricate la lățime mică rămâne deschisă exclusiv la
[Reporting](ATF_Reporting_Feedback.md#report-narrow-readability-01--tabele-imbricate-la-lățime-mică);
existența scroll-ului nu înseamnă acceptanță vizuală.
