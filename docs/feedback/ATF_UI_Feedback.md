# ATF UI — feedback pentru runtime-ul generic

J0 deține runtime-ul browser generic și testele native din `atf-ui`.
Consumerii dețin aplicațiile, page objects și oracolele business.

## UI-SUITE-TIMING-161 — stabilitatea bugetelor în suita completă

**Prioritate: 82/100** = impact 31 + lacună 26 + lucrabil 17 + documentare 8.
**Owner:** `atf-ui`; fără schimbarea timeout-urilor ori a contractelor consumerilor.

Poarta completă J0 din 2026-11-08 a rulat 277 teste `atf-ui` și a raportat
2 failures plus 5 errors. Cele 34 cazuri de `PlaywrightPointerGeometryComponentTest`
au avut 3 erori; cele 23 cazuri de `PlaywrightSelectContinuityComponentTest`
au avut 2 failures și 2 errors. Același subset de 12 scenarii a trecut izolat.
Indiciile arată variație sub rulare cumulată, dar nu identifică încă un owner
de resursă sau o cauză unică.

- [ ] Măsurați ordinea claselor, CPU/GC, timpii de startup/navigare și procesele
  deținute în rularea completă; comparați cu subsetul izolat care trece.
- [ ] Localizați primul resource leak ori pasul care consumă bugetul original;
  corectați lifecycle-ul/fixture-ul fără retry, polling repetat sau praguri mărite.
- [ ] Păstrați 700 ms pentru select, 850 ms pentru pointer, 3 s pentru
  navigarea fixture-ului și 5 s pentru pregătirea geometry; trece suita completă
  și probele focalizate cu aceleași oracole.

**Închidere:** cauza este reprodusă și eliminată; suita completă și subseturile
focalizate trec cu bugetele și aserțiunile originale.
