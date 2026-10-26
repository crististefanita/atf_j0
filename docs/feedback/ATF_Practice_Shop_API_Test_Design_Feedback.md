# Practice Shop API — diagnostic și acceptanță vizuală

## SHOP-API-FAILURE-ARTIFACT-02 — login fără răspuns și fără raport

**Prioritate:** 66/100 = impact 25 + lacună 19 + lucrabil 16 + documentare 6.
`ShopStandaloneApiReportTest` execută login-ul înaintea publicării artifactului.
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

## SHOP-API-RENDER-01 — audit interactiv al raportului standalone

**Stare: deschis pentru randarea interactivă a artifactului consumerului.**
O probă HTTP anterioară nu garantează disponibilitatea transportului curent.
Contractul stabil și limitele probelor sunt în
[README-ul Shop](../../atf-test-practice-shop/README.md#boundary-ul-probelor-api).

**Prioritate:** 33/100 = impact 10 + lacună 5 + lucrabil 15 + documentare 3.

- [ ] Deschideți un artifact standalone proaspăt în TAB MCP/ATF Debug, servit pe
      loopback dacă accesul `file:` este blocat; nu folosiți browserul Windows.
- [ ] Comparați request/response/execution și contract validation cu modelul
      comun Reporting, inclusiv `AUTH PROFILE`/`AUTH TYPE`, body, retry și
      disclosure-urile implicit colapsate.
- [ ] Verificați interactiv scroll-ul local, glyph/ARIA, redeschiderea părinților
      și navigarea Fails → Tests, fără overflow orizontal sau detalii duplicate.

Inspecția conținutului HTML și succesul componentelor nu închid acest audit.
Nu este necesară repetarea mutațiilor remote pentru verificarea rendererului:
invarianta comună are fixture neutră la Reporting, iar consumerul confirmă
wiring-ul pe artifactul propriu.

Credentialele rămân în fixture-ul local, nu în documentație sau artifact.
Rularea HTTP rămâne opt-in; gate-ul component nu contactează aplicația externă.
